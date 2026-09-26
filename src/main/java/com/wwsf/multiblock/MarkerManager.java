package com.wwsf.multiblock;

import java.io.File;
import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.wwsf.WWSFPlugin;
import com.wwsf.artillery.ArtilleryRegistry;

public final class MarkerManager {

    private MarkerManager() {}

    public record MarkerData(
        UUID markerId,
        Location core,
        CannonDefinition definition,
        int rotation,
        java.util.UUID vesselId
    ) {}

    private static final Map<UUID, MarkerData> markers = new HashMap<>();
    private static final Object LOCK = new Object();
    private static final String DATA_FILE = "markers.yml";
    private static final double LABEL_VIEW_DISTANCE = 10.0;
    private static volatile BukkitTask labelProximityTask;

    /**
     * Loads persisted markers from disk. Call once on plugin enable AFTER cannon
     * definitions have been registered (so definitions can be resolved by id).
     */
    public static void loadFromDisk() {
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        if (plugin == null) {
            return;
        }
        File file = new File(plugin.getDataFolder(), DATA_FILE);
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        var section = config.getConfigurationSection("markers");
        if (section == null) {
            return;
        }
        int loaded = 0;
        int skipped = 0;
        synchronized (LOCK) {
            for (String key : section.getKeys(false)) {
                var entry = section.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                try {
                    UUID markerId = UUID.fromString(key);
                    String worldName = entry.getString("world");
                    World world = worldName != null ? Bukkit.getWorld(worldName) : null;
                    if (world == null) {
                        skipped++;
                        continue;
                    }
                    Location core = new Location(
                        world,
                        entry.getDouble("x"),
                        entry.getDouble("y"),
                        entry.getDouble("z")
                    );
                    String defId = entry.getString("definition");
                    CannonDefinition definition = defId != null ? ArtilleryRegistry.getCannonDefinition(defId) : null;
                    if (definition == null) {
                        skipped++;
                        continue;
                    }
                    int rotation = entry.getInt("rotation");
                    String vesselRaw = entry.getString("vessel");
                    UUID vesselId = vesselRaw != null ? UUID.fromString(vesselRaw) : null;
                    markers.put(markerId, new MarkerData(markerId, core, definition, rotation, vesselId));
                    loaded++;
                } catch (Exception e) {
                    skipped++;
                    plugin.getLogger().warning("[Marker] Failed to load marker '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("[Marker] Loaded " + loaded + " marker(s) from disk (" + skipped + " skipped).");
    }

    /**
     * Persists all in-memory markers to disk so they survive server restarts and crashes.
     */
    public static void saveToDisk() {
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        if (plugin == null) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        synchronized (LOCK) {
            for (MarkerData data : markers.values()) {
                Location core = data.core();
                if (core == null || core.getWorld() == null) {
                    continue;
                }
                String path = "markers." + data.markerId();
                config.set(path + ".world", core.getWorld().getName());
                config.set(path + ".x", core.getX());
                config.set(path + ".y", core.getY());
                config.set(path + ".z", core.getZ());
                config.set(path + ".definition", data.definition().getId());
                config.set(path + ".rotation", data.rotation());
                config.set(path + ".vessel", data.vesselId() != null ? data.vesselId().toString() : null);
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(new File(plugin.getDataFolder(), DATA_FILE));
        } catch (Exception e) {
            plugin.getLogger().warning("[Marker] Failed to save markers to disk: " + e.getMessage());
        }
    }

    public static void spawnMarker(@Nonnull Location core, @Nonnull CannonDefinition definition, int rotation, @Nullable java.util.UUID vesselId) {
        World world = core.getWorld();
        if (world == null) {
            return;
        }

        removeMarkerAt(core, definition);

        int[] rotatedTrigger = definition.rotateOffset(definition.triggerOffsetX(), definition.triggerOffsetZ(), rotation);
        Location triggerLoc = core.clone().add(rotatedTrigger[0], definition.triggerOffsetY(), rotatedTrigger[1]);

        ArmorStand marker = world.spawn(triggerLoc.add(0.5, 0.5, 0.5), ArmorStand.class, as -> {
            as.setVisible(false);
            as.setMarker(true);
            as.setGravity(false);
            as.setCustomName("§8[§f" + definition.getName() + "§8]");
            as.setCustomNameVisible(false);
            as.setInvulnerable(true);
            as.setCollidable(false);
            as.setPersistent(true);
            as.setRemoveWhenFarAway(false);
            as.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        });

        synchronized (LOCK) {
            markers.put(marker.getUniqueId(), new MarkerData(marker.getUniqueId(), core.clone(), definition, rotation, vesselId));
        }
        saveToDisk();

        WWSFPlugin.getInstance().getLogger().info("[Marker] Spawned marker for " + definition.getName() + " at " + formatLocation(triggerLoc) + (vesselId != null ? " on vessel " + vesselId : ""));
    }

    public static void removeMarker(@Nonnull UUID markerId) {
        synchronized (LOCK) {
            MarkerData data = markers.remove(markerId);
            if (data != null) {
                removeMarkerEntity(markerId, data);
            }
        }
        saveToDisk();
    }

    /**
     * Removes the armor-stand entity for a marker, loading its chunk first if the
     * entity is not currently resolvable (e.g. after a restart while the chunk is unloaded).
     */
    private static void removeMarkerEntity(@Nonnull UUID markerId, @Nullable MarkerData data) {
        Entity entity = Bukkit.getEntity(markerId);
        if (entity != null) {
            entity.remove();
            return;
        }
        if (data == null) {
            return;
        }
        Location triggerLoc = computeTriggerLocation(data);
        if (triggerLoc == null || triggerLoc.getWorld() == null) {
            return;
        }
        Chunk chunk = triggerLoc.getChunk();
        boolean wasLoaded = chunk.isLoaded();
        if (!wasLoaded) {
            chunk.load();
        }
        for (Entity e : chunk.getEntities()) {
            if (e.getUniqueId().equals(markerId)) {
                e.remove();
                break;
            }
        }
    }

    @Nullable
    private static Location computeTriggerLocation(@Nonnull MarkerData data) {
        CannonDefinition def = data.definition();
        int[] rotated = def.rotateOffset(def.triggerOffsetX(), def.triggerOffsetZ(), data.rotation());
        return data.core().clone().add(rotated[0], def.triggerOffsetY(), rotated[1]);
    }

    public static void removeMarkerAt(@Nonnull Location core, @Nonnull CannonDefinition definition) {
        boolean changed = false;
        synchronized (LOCK) {
            Iterator<Map.Entry<UUID, MarkerData>> it = markers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, MarkerData> entry = it.next();
                MarkerData data = entry.getValue();
                if (data.definition().getId().equals(definition.getId())
                    && data.core().getBlockX() == core.getBlockX()
                    && data.core().getBlockY() == core.getBlockY()
                    && data.core().getBlockZ() == core.getBlockZ()) {
                    removeMarkerEntity(entry.getKey(), data);
                    it.remove();
                    changed = true;
                }
            }
        }
        if (changed) {
            saveToDisk();
        }
    }

    @Nullable
    public static MarkerData getMarkerAt(@Nonnull Location core, @Nonnull CannonDefinition definition) {
        synchronized (LOCK) {
            for (MarkerData data : markers.values()) {
                if (data.definition().getId().equals(definition.getId())
                    && data.core().getBlockX() == core.getBlockX()
                    && data.core().getBlockY() == core.getBlockY()
                    && data.core().getBlockZ() == core.getBlockZ()) {
                    return data;
                }
            }
            return null;
        }
    }

    @Nullable
    public static MarkerData findMarkerForBlock(@Nonnull Location block) {
        synchronized (LOCK) {
            for (MarkerData data : markers.values()) {
                if (isBlockInMultiblock(block, data.core(), data.definition(), data.rotation())) {
                    return data;
                }
            }
            return null;
        }
    }

    public static boolean isBlockInMultiblock(
        @Nonnull Location block,
        @Nonnull Location core,
        @Nonnull CannonDefinition definition,
        int rotation
    ) {
        int ox = core.getBlockX();
        int oy = core.getBlockY();
        int oz = core.getBlockZ();
        int wx = block.getBlockX();
        int wy = block.getBlockY();
        int wz = block.getBlockZ();

        int worldOffsetX = wx - ox;
        int worldOffsetZ = wz - oz;

        int[] local = inverseRotate(worldOffsetX, worldOffsetZ, rotation);
        int localX = local[0];
        int localZ = local[1];
        int localY = wy - oy;

        int sizeX = definition.getSizeX();
        int sizeY = definition.getSizeY();
        int sizeZ = definition.getSizeZ();

        if (localX < 0 || localX >= sizeX || localY < 0 || localY >= sizeY || localZ < 0 || localZ >= sizeZ) {
            return false;
        }

        if (localX == definition.triggerOffsetX() && localY == definition.triggerOffsetY() && localZ == definition.triggerOffsetZ()) {
            return false;
        }

        Material expected = definition.getMaterialAt(localX, localY, localZ);
        return expected != Material.AIR && expected == block.getBlock().getType();
    }

    public static boolean isCoreValid(@Nonnull MarkerData data) {
        CannonDefinition def = data.definition();
        int[] triggerRotated = def.rotateOffset(def.triggerOffsetX(), def.triggerOffsetZ(), data.rotation());
        Location triggerLoc = data.core().clone().add(triggerRotated[0], def.triggerOffsetY(), triggerRotated[1]);
        return def.isValid(triggerLoc.getBlock());
    }

    public static void cleanupInvalid() {
        boolean changed = false;
        synchronized (LOCK) {
            Iterator<Map.Entry<UUID, MarkerData>> it = markers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, MarkerData> entry = it.next();
                if (!isCoreValid(entry.getValue())) {
                    removeMarkerEntity(entry.getKey(), entry.getValue());
                    it.remove();
                    changed = true;
                }
            }
        }
        if (changed) {
            saveToDisk();
        }
    }

    @Nullable
    public static MarkerData getMarkerData(@Nonnull UUID markerId) {
        synchronized (LOCK) {
            return markers.get(markerId);
        }
    }

    @Nullable
    public static MarkerData findMarkerAtLocation(@Nonnull Location loc) {
        synchronized (LOCK) {
            for (MarkerData data : markers.values()) {
                if (data.core().getBlockX() == loc.getBlockX()
                    && data.core().getBlockY() == loc.getBlockY()
                    && data.core().getBlockZ() == loc.getBlockZ()) {
                    return data;
                }
            }
            return null;
        }
    }

    @Nullable
    public static MarkerData findMarkerAtEntity(@Nonnull Location loc) {
        synchronized (LOCK) {
            for (MarkerData data : markers.values()) {
                Entity entity = Bukkit.getEntity(data.markerId());
                if (entity != null && entity.getLocation().getBlockX() == loc.getBlockX()
                    && entity.getLocation().getBlockY() == loc.getBlockY()
                    && entity.getLocation().getBlockZ() == loc.getBlockZ()) {
                    return data;
                }
            }
            return null;
        }
    }

    public static void moveMarkersForVessel(@Nonnull UUID vesselId, @Nonnull Vector delta) {
        if (delta.lengthSquared() < 0.001) {
            return;
        }
        synchronized (LOCK) {
            for (MarkerData data : markers.values()) {
                if (vesselId.equals(data.vesselId())) {
                    data.core().add(delta);
                    Entity entity = Bukkit.getEntity(data.markerId());
                    if (entity != null) {
                        entity.teleport(entity.getLocation().add(delta));
                    }
                }
            }
        }
    }

    public static void clearAllForVessel(@Nonnull UUID vesselId) {
        boolean changed = false;
        synchronized (LOCK) {
            Iterator<Map.Entry<UUID, MarkerData>> it = markers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, MarkerData> entry = it.next();
                if (vesselId.equals(entry.getValue().vesselId())) {
                    removeMarkerEntity(entry.getKey(), entry.getValue());
                    it.remove();
                    changed = true;
                }
            }
        }
        if (changed) {
            saveToDisk();
        }
    }

    public static void clearAll() {
        synchronized (LOCK) {
            for (MarkerData data : markers.values()) {
                Entity entity = Bukkit.getEntity(data.markerId());
                if (entity != null) {
                    entity.remove();
                }
            }
            markers.clear();
        }
        stopLabelProximityUpdater();
    }

    public static void startLabelProximityUpdater() {
        stopLabelProximityUpdater();
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        if (plugin == null) {
            return;
        }
        labelProximityTask = plugin.getServer().getScheduler().runTaskTimer(plugin, MarkerManager::updateLabelVisibility, 40L, 40L);
    }

    public static void stopLabelProximityUpdater() {
        BukkitTask task = labelProximityTask;
        if (task != null) {
            task.cancel();
            labelProximityTask = null;
        }
    }

    private static void updateLabelVisibility() {
        java.util.Collection<MarkerData> snapshot;
        synchronized (LOCK) {
            if (markers.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(markers.values());
        }
        for (MarkerData data : snapshot) {
            Entity entity = Bukkit.getEntity(data.markerId());
            if (!(entity instanceof ArmorStand stand)) {
                continue;
            }
            Location markerLoc = stand.getLocation();
            boolean nearby = false;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(markerLoc.getWorld())
                        && markerLoc.distance(player.getLocation()) <= LABEL_VIEW_DISTANCE) {
                    nearby = true;
                    break;
                }
            }
            stand.setCustomNameVisible(nearby);
        }
    }

    private static int[] inverseRotate(int cx, int cz, int rotation) {
        return switch (rotation) {
            case 0 -> new int[]{cx, cz};
            case 1 -> new int[]{-cz, cx};
            case 2 -> new int[]{-cx, -cz};
            case 3 -> new int[]{cz, -cx};
            default -> new int[]{cx, cz};
        };
    }

    private static String formatLocation(Location loc) {
        return loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }
}
