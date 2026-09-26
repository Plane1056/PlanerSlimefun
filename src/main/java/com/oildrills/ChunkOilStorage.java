package com.oildrills;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Geographic oil-field storage.
 *
 * <p>The class keeps its historical name and chunk overloads for binary compatibility, but oil
 * is no longer generated or stored per chunk. Exact locations resolve against the ellipses in
 * {@code oil_fields.json}; every drill in a field drains that field's shared persistent reserve.</p>
 */
public class ChunkOilStorage {

    private static final String DATABASE_RESOURCE = "oil_fields.json";
    private static final String STATE_FILE = "oil_field_state.yml";
    private static final String LEGACY_FILE = "chunk_oil.yml";

    private final JavaPlugin plugin;
    private final Map<String, OilFieldDefinition> fieldsById = new LinkedHashMap<>();
    private final List<OilFieldDefinition> fields = new ArrayList<>();
    private final Map<String, Integer> remaining = new HashMap<>();
    private final Set<String> surveyed = new HashSet<>();
    private final Set<String> enabledWorlds = new HashSet<>();

    private File databaseFile;
    private File stateFile;
    private int barrelsPerScore;
    private boolean legacySurveyMigrationComplete;

    public ChunkOilStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), DATABASE_RESOURCE);
        this.stateFile = new File(plugin.getDataFolder(), STATE_FILE);
    }

    /**
     * Legacy constructor retained for addons compiled against WWSF2 2.0.0.
     * The old per-chunk maximum is intentionally ignored.
     */
    public ChunkOilStorage(JavaPlugin plugin, int ignoredMaxBarrelsPerChunk) {
        this(plugin);
    }

    public synchronized void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().severe("Could not create the WWSF2 data folder.");
            return;
        }
        if (!databaseFile.exists()) {
            plugin.saveResource(DATABASE_RESOURCE, false);
        }

        barrelsPerScore = Math.max(1, plugin.getConfig().getInt("oil-fields.barrels-per-score", 60));
        enabledWorlds.clear();
        for (String worldName : plugin.getConfig().getStringList("oil-fields.worlds")) {
            if (worldName != null && !worldName.isBlank()) {
                enabledWorlds.add(worldName.toLowerCase(Locale.ROOT));
            }
        }

        loadDefinitions();
        loadState();
        migrateLegacySurveys();

        plugin.getLogger().info("Loaded " + fields.size() + " geographic oil fields from "
                + DATABASE_RESOURCE + ". Oil is field-based; random chunk generation is disabled.");
    }

    private void loadDefinitions() {
        fields.clear();
        fieldsById.clear();

        try (Reader reader = new FileReader(databaseFile, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                throw new IllegalArgumentException("Database root must be a JSON object");
            }
            JsonObject root = rootElement.getAsJsonObject();
            JsonArray definitions = root.getAsJsonArray("fields");
            if (definitions == null) {
                throw new IllegalArgumentException("Database has no 'fields' array");
            }

            for (int index = 0; index < definitions.size(); index++) {
                JsonElement element = definitions.get(index);
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("Field " + index + " is not an object");
                }
                OilFieldDefinition field = OilFieldDefinition.fromJson(element.getAsJsonObject());
                if (fieldsById.putIfAbsent(field.id(), field) != null) {
                    throw new IllegalArgumentException("Duplicate field id '" + field.id() + "'");
                }
                fields.add(field);
            }
            if (fields.isEmpty()) {
                throw new IllegalArgumentException("Database contains no oil fields");
            }
        } catch (IOException | RuntimeException ex) {
            fields.clear();
            fieldsById.clear();
            plugin.getLogger().severe("Could not load " + databaseFile.getAbsolutePath() + ": " + ex.getMessage());
        }
    }

    private void loadState() {
        remaining.clear();
        surveyed.clear();
        legacySurveyMigrationComplete = false;
        if (!stateFile.exists()) {
            return;
        }

        FileConfiguration state = YamlConfiguration.loadConfiguration(stateFile);
        legacySurveyMigrationComplete = state.getBoolean("migration.chunk-surveys-imported", false);
        ConfigurationSection worldsSection = state.getConfigurationSection("worlds");
        if (worldsSection == null) {
            return;
        }

        for (String worldId : worldsSection.getKeys(false)) {
            ConfigurationSection fieldSection = worldsSection.getConfigurationSection(worldId + ".fields");
            if (fieldSection == null) {
                continue;
            }
            for (String fieldId : fieldSection.getKeys(false)) {
                OilFieldDefinition field = fieldsById.get(fieldId);
                if (field == null) {
                    plugin.getLogger().warning("Ignoring saved state for unknown oil field '" + fieldId + "'.");
                    continue;
                }
                String stateKey = worldId + ":" + fieldId;
                String path = fieldId + ".remaining";
                if (fieldSection.isInt(path)) {
                    int saved = Math.max(0, fieldSection.getInt(path));
                    remaining.put(stateKey, Math.min(saved, field.capacity(barrelsPerScore)));
                }
                if (fieldSection.getBoolean(fieldId + ".surveyed", false)) {
                    surveyed.add(stateKey);
                }
            }
        }
    }

    public synchronized void save() {
        FileConfiguration state = new YamlConfiguration();
        state.set("schema-version", 1);
        state.set("migration.chunk-surveys-imported", legacySurveyMigrationComplete);

        Set<String> keys = new HashSet<>(remaining.keySet());
        keys.addAll(surveyed);
        for (String stateKey : keys) {
            int separator = stateKey.indexOf(':');
            if (separator <= 0 || separator == stateKey.length() - 1) {
                continue;
            }
            String worldId = stateKey.substring(0, separator);
            String fieldId = stateKey.substring(separator + 1);
            String path = "worlds." + worldId + ".fields." + fieldId;
            OilFieldDefinition field = fieldsById.get(fieldId);
            if (field == null) {
                continue;
            }
            state.set(path + ".remaining", remaining.getOrDefault(stateKey, field.capacity(barrelsPerScore)));
            if (surveyed.contains(stateKey)) {
                state.set(path + ".surveyed", true);
            }
        }

        try {
            state.save(stateFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save " + STATE_FILE + ": " + ex.getMessage());
        }
    }

    private void migrateLegacySurveys() {
        if (legacySurveyMigrationComplete || fields.isEmpty()) {
            return;
        }
        File legacyFile = new File(plugin.getDataFolder(), LEGACY_FILE);
        int migrated = 0;
        if (legacyFile.exists()) {
            FileConfiguration legacy = YamlConfiguration.loadConfiguration(legacyFile);
            ConfigurationSection oldSurveys = legacy.getConfigurationSection("surveyed");
            if (oldSurveys != null) {
                for (String oldKey : oldSurveys.getKeys(false)) {
                    if (!oldSurveys.getBoolean(oldKey, false) || !migrateLegacySurvey(oldKey)) {
                        continue;
                    }
                    migrated++;
                }
            }
        }
        legacySurveyMigrationComplete = true;
        if (migrated > 0) {
            plugin.getLogger().info("Migrated " + migrated
                    + " legacy chunk surveys to geographic oil-field survey flags.");
        }
        save();
    }

    private boolean migrateLegacySurvey(String oldKey) {
        String[] parts = oldKey.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            World world = Bukkit.getWorld(UUID.fromString(parts[0]));
            if (world == null || !isWorldEnabled(world)) {
                return false;
            }
            int chunkX = Integer.parseInt(parts[1]);
            int chunkZ = Integer.parseInt(parts[2]);
            OilFieldDefinition field = findFieldAtCoordinates(
                    world,
                    chunkX * 16.0 + 7.5,
                    chunkZ * 16.0 + 7.5
            );
            if (field == null) {
                return false;
            }
            return surveyed.add(stateKey(world, field));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** Kept for compatibility with code that used the old chunk data key. */
    public static String key(Chunk chunk) {
        World world = chunk.getWorld();
        if (world == null) {
            return null;
        }
        return world.getUID() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    public synchronized int getOrGenerate(Location location) {
        ResolvedField resolved = resolve(location);
        return resolved == null ? 0 : remaining(resolved);
    }

    /**
     * Compatibility overload. New code should pass an exact location, not a chunk.
     */
    public int getOrGenerate(Chunk chunk) {
        return getOrGenerate(chunkCenter(chunk));
    }

    public synchronized boolean isSurveyed(Location location) {
        ResolvedField resolved = resolve(location);
        return resolved != null && surveyed.contains(resolved.stateKey());
    }

    public boolean isSurveyed(Chunk chunk) {
        return isSurveyed(chunkCenter(chunk));
    }

    public synchronized void markSurveyed(Location location) {
        ResolvedField resolved = resolve(location);
        if (resolved != null) {
            surveyed.add(resolved.stateKey());
            remaining(resolved);
        }
    }

    public void markSurveyed(Chunk chunk) {
        markSurveyed(chunkCenter(chunk));
    }

    public synchronized OilRarity getRarity(Location location) {
        ResolvedField resolved = resolve(location);
        if (resolved == null) {
            return OilRarity.DRY_VOID;
        }
        int gradeOnLegacyScale = (int) Math.round(resolved.field().oilAmountScore() * 5.0);
        return OilRarity.fromBarrels(gradeOnLegacyScale);
    }

    public OilRarity getRarity(Chunk chunk) {
        return getRarity(chunkCenter(chunk));
    }

    public synchronized boolean extract(Location location, int amount) {
        if (amount <= 0) {
            return amount == 0;
        }
        ResolvedField resolved = resolve(location);
        if (resolved == null) {
            return false;
        }
        int current = remaining(resolved);
        if (current < amount) {
            return false;
        }
        remaining.put(resolved.stateKey(), current - amount);
        return true;
    }

    public boolean extract(Chunk chunk, int amount) {
        return extract(chunkCenter(chunk), amount);
    }

    public synchronized void refund(Location location, int amount) {
        if (amount <= 0) {
            return;
        }
        ResolvedField resolved = resolve(location);
        if (resolved == null) {
            return;
        }
        int capacity = resolved.field().capacity(barrelsPerScore);
        long updated = (long) remaining(resolved) + amount;
        remaining.put(resolved.stateKey(), (int) Math.min(capacity, updated));
    }

    public void refund(Chunk chunk, int amount) {
        refund(chunkCenter(chunk), amount);
    }

    public synchronized boolean extractIfSurveyed(Location location, int amount) {
        ResolvedField resolved = resolve(location);
        if (resolved == null || !surveyed.contains(resolved.stateKey()) || amount < 0) {
            return false;
        }
        if (amount == 0) {
            return true;
        }
        int current = remaining(resolved);
        if (current < amount) {
            return false;
        }
        remaining.put(resolved.stateKey(), current - amount);
        return true;
    }

    public boolean extractIfSurveyed(Chunk chunk, int amount) {
        return extractIfSurveyed(chunkCenter(chunk), amount);
    }

    public synchronized int getSurveyedOrZero(Location location) {
        ResolvedField resolved = resolve(location);
        if (resolved == null || !surveyed.contains(resolved.stateKey())) {
            return 0;
        }
        return remaining(resolved);
    }

    public int getSurveyedOrZero(Chunk chunk) {
        return getSurveyedOrZero(chunkCenter(chunk));
    }

    public synchronized String getFieldName(Location location) {
        ResolvedField resolved = resolve(location);
        return resolved == null ? null : resolved.field().name();
    }

    public synchronized String getFieldId(Location location) {
        ResolvedField resolved = resolve(location);
        return resolved == null ? null : resolved.field().id();
    }

    public synchronized int getCapacity(Location location) {
        ResolvedField resolved = resolve(location);
        return resolved == null ? 0 : resolved.field().capacity(barrelsPerScore);
    }

    public synchronized int getFieldCount() {
        return fields.size();
    }

    public synchronized OilFieldTarget findNearbyField(Location location, double range) {
        if (location == null
                || location.getWorld() == null
                || !isWorldEnabled(location.getWorld())
                || range < 0.0) {
            return null;
        }

        double x = location.getX();
        double z = location.getZ();
        OilFieldDefinition field = OilFieldDefinition.findClosestContaining(fields, x, z);
        if (field == null) {
            field = OilFieldDefinition.findClosestIntersectingSquare(fields, x, z, range);
        }
        if (field == null) {
            return null;
        }

        int capacity = field.capacity(barrelsPerScore);
        int barrels = remaining(new ResolvedField(field, stateKey(location.getWorld(), field)));
        int gradeOnLegacyScale = (int) Math.round(field.oilAmountScore() * 5.0);
        return new OilFieldTarget(
                field.id(),
                field.name(),
                OilRarity.fromBarrels(gradeOnLegacyScale),
                barrels,
                capacity
        );
    }

    public synchronized boolean markSurveyedField(World world, String fieldId) {
        if (world == null || !isWorldEnabled(world)) {
            return false;
        }
        OilFieldDefinition field = fieldsById.get(fieldId);
        if (field == null) {
            return false;
        }
        String key = stateKey(world, field);
        surveyed.add(key);
        remaining.computeIfAbsent(key, ignored -> field.capacity(barrelsPerScore));
        return true;
    }

    private int remaining(ResolvedField resolved) {
        return remaining.computeIfAbsent(
                resolved.stateKey(),
                ignored -> resolved.field().capacity(barrelsPerScore)
        );
    }

    private ResolvedField resolve(Location location) {
        if (location == null || location.getWorld() == null || !isWorldEnabled(location.getWorld())) {
            return null;
        }
        OilFieldDefinition field = findFieldAtBlock(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockZ()
        );
        return field == null ? null : new ResolvedField(field, stateKey(location.getWorld(), field));
    }

    private OilFieldDefinition findFieldAtBlock(World world, int blockX, int blockZ) {
        if (world == null || !isWorldEnabled(world)) {
            return null;
        }
        return OilFieldDefinition.findClosestContainingBlock(fields, blockX, blockZ);
    }

    private OilFieldDefinition findFieldAtCoordinates(World world, double x, double z) {
        if (world == null || !isWorldEnabled(world)) {
            return null;
        }
        return OilFieldDefinition.findClosestContaining(fields, x, z);
    }

    private boolean isWorldEnabled(World world) {
        if (!enabledWorlds.isEmpty()) {
            return enabledWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
        }
        return world.getEnvironment() == World.Environment.NORMAL;
    }

    private static String stateKey(World world, OilFieldDefinition field) {
        return world.getUID() + ":" + field.id();
    }

    private static Location chunkCenter(Chunk chunk) {
        // resolve(Location) adds 0.5 to reach a block centre, so use the lower of the two
        // central blocks to represent the exact 7.5/7.5 centre of this 16x16 chunk.
        return new Location(chunk.getWorld(), chunk.getX() * 16.0 + 7.0, 0.0, chunk.getZ() * 16.0 + 7.0);
    }

    private record ResolvedField(OilFieldDefinition field, String stateKey) {
    }

    public record OilFieldTarget(
            String fieldId,
            String name,
            OilRarity rarity,
            int remaining,
            int capacity
    ) {
    }
}
