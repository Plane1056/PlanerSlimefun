package com.oildrills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/** Saves active drill locations so they keep running after restart. */
final class DrillPersistence {

    private final JavaPlugin plugin;
    private final File file;

    DrillPersistence(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "active_drills.yml");
    }

    Map<Location, ActiveDrillData> load() {
        Map<Location, ActiveDrillData> map = new HashMap<>();
        if (!file.exists()) {
            return map;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (!cfg.contains("drills")) {
            return map;
        }

        for (String key : cfg.getConfigurationSection("drills").getKeys(false)) {
            String path = "drills." + key;
            String worldName = cfg.getString(path + ".world");
            if (worldName == null) {
                continue;
            }
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            int x = cfg.getInt(path + ".x");
            int y = cfg.getInt(path + ".y");
            int z = cfg.getInt(path + ".z");
            String tierName = cfg.getString(path + ".tier", "MARK_I");
            long next = cfg.getLong(path + ".next-extract-ms", System.currentTimeMillis());
            boolean enabled = cfg.getBoolean(path + ".enabled", false);
            BlockFace facing = parseFacing(cfg.getString(path + ".facing"));
            int fuelCredit = Math.max(0, cfg.getInt(path + ".fuel-credit", 0));

            OilDrillTier tier;
            try {
                tier = OilDrillTier.valueOf(tierName);
            } catch (IllegalArgumentException ex) {
                tier = OilDrillTier.MARK_I;
            }

            map.put(
                new Location(world, x, y, z),
                new ActiveDrillData(tier, next, enabled, facing, fuelCredit)
            );
        }
        return map;
    }

    void save(Map<Location, ActiveDrillData> drills) {
        FileConfiguration cfg = new YamlConfiguration();
        int i = 0;
        for (Map.Entry<Location, ActiveDrillData> entry : drills.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null) {
                continue;
            }
            String path = "drills." + i;
            cfg.set(path + ".world", loc.getWorld().getName());
            cfg.set(path + ".x", loc.getBlockX());
            cfg.set(path + ".y", loc.getBlockY());
            cfg.set(path + ".z", loc.getBlockZ());
            cfg.set(path + ".tier", entry.getValue().tier().name());
            cfg.set(path + ".next-extract-ms", entry.getValue().nextExtractAtMs());
            cfg.set(path + ".enabled", entry.getValue().isEnabled());
            if (entry.getValue().fuelCredit() > 0) {
                cfg.set(path + ".fuel-credit", entry.getValue().fuelCredit());
            }
            BlockFace facing = entry.getValue().facing();
            if (facing != null) {
                cfg.set(path + ".facing", facing.name());
            }
            i++;
        }
        try {
            Files.createDirectories(file.toPath().getParent());
            Path target = file.toPath();
            Path temporary = target.resolveSibling(file.getName() + ".tmp");
            Files.writeString(
                temporary,
                cfg.saveToString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save active_drills.yml: " + ex.getMessage());
        }
    }

    private static BlockFace parseFacing(String value) {
        if (value == null) {
            return null;
        }
        try {
            BlockFace facing = BlockFace.valueOf(value);
            return switch (facing) {
                case NORTH, EAST, SOUTH, WEST -> facing;
                default -> null;
            };
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
