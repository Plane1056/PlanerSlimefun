package com.wwsf.multiblock;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.wwsf.WWSFPlugin;

public final class MaximMachineGunLoader {

    private static final String RESOURCE_PATH = "maxim_machine_gun.yml";
    private static final String NBT_FILENAME = "Max_Machine_Gun.nbt";
    private static final String NBT_SCHEMATIC_PATH = "schematics" + File.separator + NBT_FILENAME;

    private MaximMachineGunLoader() {}

    @Nullable
    public static MaximMachineGunDefinition load() {
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        java.util.Map<Character, Material> yamlPalette = readPaletteFromYaml();

        List<File> candidates = new ArrayList<>();
        File pluginDataFolder = plugin.getDataFolder();
        candidates.add(new File(pluginDataFolder, NBT_SCHEMATIC_PATH));
        File serverRoot = pluginDataFolder.getParentFile();
        if (serverRoot != null) {
            File serverParent = serverRoot.getParentFile();
            if (serverParent != null) {
                candidates.add(new File(serverParent, "schematics" + File.separator + NBT_FILENAME));
            }
        }

        for (File candidate : candidates) {
            if (candidate.exists() && yamlPalette != null && !yamlPalette.isEmpty()) {
                try {
                    plugin.getLogger().info("[MaximGun] Loading structure from NBT: " + candidate.getAbsolutePath());
                    return loadFromNbt(candidate, yamlPalette);
                } catch (Exception e) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING,
                            "[MaximGun] NBT load failed, falling back to YAML", e);
                }
            }
        }

        InputStream in = plugin.getResource(RESOURCE_PATH);
        if (in == null) {
            plugin.getLogger().severe("Missing resource: " + RESOURCE_PATH);
            return null;
        }

        try (in) {
            return loadFromYaml(in);
        } catch (Exception e) {
            WWSFPlugin.getInstance().getLogger().log(java.util.logging.Level.SEVERE,
                "Failed to load " + RESOURCE_PATH, e);
            return null;
        }
    }

    @Nullable
    private static java.util.Map<Character, Material> readPaletteFromYaml() {
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        InputStream in = plugin.getResource(RESOURCE_PATH);
        if (in == null) return null;
        try (in) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
            ConfigurationSection root = yaml.getConfigurationSection("maxim_machine_gun");
            if (root == null) return null;
            ConfigurationSection paletteSection = root.getConfigurationSection("palette");
            if (paletteSection == null) return null;
            java.util.Map<Character, Material> palette = new java.util.HashMap<>();
            for (String key : paletteSection.getKeys(false)) {
                if (key.length() != 1) continue;
                char c = key.charAt(0);
                String matStr = paletteSection.getString(key);
                Material mat = Material.matchMaterial(matStr != null ? matStr : "");
                if (mat != null) palette.put(c, mat);
            }
            return palette;
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Failed to read YAML palette for Maxim Gun NBT fallback", e);
            return null;
        }
    }

    @Nullable
    static MaximMachineGunDefinition loadFromNbt(File nbtFile, java.util.Map<Character, Material> yamlPalette) throws Exception {
        NbtStructureReader.LayerResult layers = NbtStructureReader.readNbtLayers(nbtFile, yamlPalette);

        String displayName = "Maxim Machine Gun";
        Material triggerMaterial = Material.DARK_OAK_FENCE_GATE;
        int triggerX = 0, triggerY = 1, triggerZ = 1;
        int barrelTipX = layers.sizeX - 1, barrelTipY = 1, barrelTipZ = 1;
        String onCompleteEvent = "MaximMachineGunDetectedEvent";

        return new MaximMachineGunDefinition(
            displayName, layers.sizeX, layers.sizeY, layers.sizeZ,
            triggerMaterial,
            triggerX, triggerY, triggerZ,
            barrelTipX, barrelTipY, barrelTipZ,
            yamlPalette,
            layers.layerRows,
            onCompleteEvent
        );
    }

    @Nullable
    private static MaximMachineGunDefinition loadFromYaml(InputStream in) throws Exception {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
        ConfigurationSection root = yaml.getConfigurationSection("maxim_machine_gun");
        if (root == null) {
            WWSFPlugin.getInstance().getLogger().severe("Missing 'maxim_machine_gun:' root section in " + RESOURCE_PATH);
            return null;
        }

        String displayName = root.getString("name", "Maxim Machine Gun");

        ConfigurationSection size = root.getConfigurationSection("size");
        if (size == null) {
            WWSFPlugin.getInstance().getLogger().severe("Missing 'size' section in " + RESOURCE_PATH);
            return null;
        }
        int sizeX = size.getInt("x", 5);
        int sizeY = size.getInt("y", 3);
        int sizeZ = size.getInt("z", 3);

        List<Integer> barrelTip = root.getIntegerList("barrel_tip");
        if (barrelTip.size() < 3) {
            WWSFPlugin.getInstance().getLogger().severe("Missing 'barrel_tip [x,y,z]' in " + RESOURCE_PATH);
            return null;
        }

        ConfigurationSection triggerSection = root.getConfigurationSection("trigger");
        if (triggerSection == null) {
            WWSFPlugin.getInstance().getLogger().severe("Missing 'trigger' section in " + RESOURCE_PATH);
            return null;
        }
        Material triggerMaterial = Material.matchMaterial(triggerSection.getString("material", ""));
        if (triggerMaterial == null) {
            WWSFPlugin.getInstance().getLogger().severe("Invalid trigger material in " + RESOURCE_PATH);
            return null;
        }
        List<Integer> triggerPos = triggerSection.getIntegerList("offset");
        if (triggerPos.size() < 3) {
            WWSFPlugin.getInstance().getLogger().severe("Trigger offset must have 3 elements [x, y, z]");
            return null;
        }

        ConfigurationSection paletteSection = root.getConfigurationSection("palette");
        if (paletteSection == null) {
            WWSFPlugin.getInstance().getLogger().severe("Missing 'palette' section in " + RESOURCE_PATH);
            return null;
        }
        Map<Character, Material> palette = new HashMap<>();
        for (String key : paletteSection.getKeys(false)) {
            if (key.length() != 1) {
                WWSFPlugin.getInstance().getLogger().warning("Skipping multi-character palette key: " + key);
                continue;
            }
            char c = key.charAt(0);
            String matStr = paletteSection.getString(key);
            Material mat = Material.matchMaterial(matStr != null ? matStr : "");
            if (mat == null) {
                WWSFPlugin.getInstance().getLogger().warning("Unknown material '" + matStr + "' for key '" + c + "' — skipping");
                continue;
            }
            palette.put(c, mat);
        }

        ConfigurationSection layersSection = root.getConfigurationSection("layers");
        if (layersSection == null) {
            WWSFPlugin.getInstance().getLogger().severe("Missing 'layers' section in " + RESOURCE_PATH);
            return null;
        }

        Map<Integer, String[]> layerRows = new HashMap<>();
        for (String key : layersSection.getKeys(false)) {
            int yIndex;
            try {
                yIndex = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                WWSFPlugin.getInstance().getLogger().warning("Skipping non-integer layer key: " + key);
                continue;
            }

            ConfigurationSection layer = layersSection.getConfigurationSection(key);
            if (layer == null) continue;

            List<String> rows = layer.getStringList("rows");
            if (rows.isEmpty()) {
                WWSFPlugin.getInstance().getLogger().warning("Layer " + yIndex + " has no rows — skipping");
                continue;
            }

            if (rows.size() != sizeZ) {
                WWSFPlugin.getInstance().getLogger().warning("Layer " + yIndex + " has " + rows.size()
                    + " rows but expected " + sizeZ + " (sizeZ)");
                return null;
            }
            for (int i = 0; i < rows.size(); i++) {
                String row = rows.get(i);
                if (row.length() != sizeX) {
                    WWSFPlugin.getInstance().getLogger().warning("Layer " + yIndex + " row " + i
                        + " has " + row.length() + " columns but expected " + sizeX + " (sizeX)");
                    return null;
                }
            }

            layerRows.put(yIndex, rows.toArray(new String[0]));
        }

        String onCompleteEvent = root.getString("on_complete_event", "MaximMachineGunDetectedEvent");

        return new MaximMachineGunDefinition(
            displayName, sizeX, sizeY, sizeZ,
            triggerMaterial,
            triggerPos.get(0), triggerPos.get(1), triggerPos.get(2),
            barrelTip.get(0), barrelTip.get(1), barrelTip.get(2),
            palette,
            layerRows,
            onCompleteEvent
        );
    }
}
