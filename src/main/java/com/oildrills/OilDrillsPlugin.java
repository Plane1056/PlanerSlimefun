package com.oildrills;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class OilDrillsPlugin {

    private static OilDrillsPlugin instance;
    private final JavaPlugin plugin;
    private ChunkOilStorage oilStorage;
    private DrillManager drillManager;

    public OilDrillsPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public void enable() {
        if (Bukkit.getPluginManager().getPlugin("Slimefun") == null) {
            plugin.getLogger().severe("Slimefun is required. Oil Drills portion disabled.");
            return;
        }

        migrateLegacyOilBalance();

        oilStorage = new ChunkOilStorage(plugin);
        oilStorage.load();

        OilItems.register(plugin);
        Bukkit.getPluginManager().registerEvents(new DowsingRodListener(oilStorage), plugin);
        plugin.getLogger().info("Dowsing Rod listener registered (50-block oil-field detection).");

        drillManager = new DrillManager(plugin, oilStorage);
        drillManager.start();

        Bukkit.getPluginManager().registerEvents(new FuelHandler(), plugin);

        plugin.getLogger().info("Oil Drills enabled with geographic field storage and Dowsing Rod discovery.");
    }

    public void disable() {
        OilRecipeRegistry.unregister();
        if (drillManager != null) {
            drillManager.shutdown();
        }
        if (oilStorage != null) {
            oilStorage.save();
        }
        plugin.getLogger().info("Oil Drills disabled.");
    }

    private void migrateLegacyOilBalance() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("oil-fields.balance-version", true)) {
            return;
        }

        if (config.getInt("oil-fields.barrels-per-score", 2000) == 2000) {
            config.set("oil-fields.barrels-per-score", 60);
        }
        config.set("oil-fields.balance-version", 2);
        plugin.saveConfig();
        plugin.getLogger().info("Migrated oil-field reserves to the 6,000-barrel score-100 scale.");
    }

    public static OilDrillsPlugin getInstance() {
        return instance;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Logger getLogger() {
        return plugin.getLogger();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public ChunkOilStorage getOilStorage() {
        return oilStorage;
    }

    public DrillManager getDrillManager() {
        return drillManager;
    }
}
