package com.wwsf.artillery;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

import com.wwsf.WWSFPlugin;

/**
 * Reads per-shell tuning from {@code shells.<TYPE>.<key>} in {@code config.yml}, following the same
 * config-driven stat override pattern used elsewhere by {@link com.wwsf.config.ItemConfigHelper}.
 */
public final class ShellConfig {

    private ShellConfig() {
    }

    private static FileConfiguration config() {
        return WWSFPlugin.getInstance().getConfig();
    }

    public static double getDouble(ShellType type, String key, double defaultValue) {
        return config().getDouble("shells." + type.name() + "." + key, defaultValue);
    }

    public static int getInt(ShellType type, String key, int defaultValue) {
        return config().getInt("shells." + type.name() + "." + key, defaultValue);
    }

    public static boolean getBoolean(ShellType type, String key, boolean defaultValue) {
        return config().getBoolean("shells." + type.name() + "." + key, defaultValue);
    }

    public static List<String> getStringList(ShellType type, String key) {
        return config().getStringList("shells." + type.name() + "." + key);
    }
}
