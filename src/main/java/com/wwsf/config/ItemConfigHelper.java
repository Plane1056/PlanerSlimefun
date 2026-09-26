package com.wwsf.config;

import javax.annotation.Nonnull;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.wwsf.WWSFPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * Per-item tuning from {@code items.<id>.<key>} in {@code config.yml}.
 */
public final class ItemConfigHelper {

    private ItemConfigHelper() {
    }

    @Nonnull
    public static FileConfiguration getItemConfig(@Nonnull SlimefunItem item) {
        FileConfiguration view = new YamlConfiguration();
        ConfigurationSection section = pluginConfig().getConfigurationSection("items." + item.getId());
        if (section == null) {
            return view;
        }
        for (String key : section.getKeys(true)) {
            view.set(key, section.get(key));
        }
        return view;
    }

    public static double getDouble(@Nonnull SlimefunItem item, @Nonnull String key, double defaultValue) {
        return pluginConfig().getDouble(path(item.getId(), key), defaultValue);
    }

    public static int getInt(@Nonnull SlimefunItem item, @Nonnull String key, int defaultValue) {
        return pluginConfig().getInt(path(item.getId(), key), defaultValue);
    }

    public static long getLong(@Nonnull SlimefunItem item, @Nonnull String key, long defaultValue) {
        return pluginConfig().getLong(path(item.getId(), key), defaultValue);
    }

    public static boolean getBoolean(@Nonnull SlimefunItem item, @Nonnull String key, boolean defaultValue) {
        return pluginConfig().getBoolean(path(item.getId(), key), defaultValue);
    }

    public static double getDouble(@Nonnull String itemId, @Nonnull String key, double defaultValue) {
        return pluginConfig().getDouble(path(itemId, key), defaultValue);
    }

    public static int getInt(@Nonnull String itemId, @Nonnull String key, int defaultValue) {
        return pluginConfig().getInt(path(itemId, key), defaultValue);
    }

    public static long getLong(@Nonnull String itemId, @Nonnull String key, long defaultValue) {
        return pluginConfig().getLong(path(itemId, key), defaultValue);
    }

    public static boolean getBoolean(@Nonnull String itemId, @Nonnull String key, boolean defaultValue) {
        return pluginConfig().getBoolean(path(itemId, key), defaultValue);
    }

    private static FileConfiguration pluginConfig() {
        return WWSFPlugin.getInstance().getConfig();
    }

    private static String path(@Nonnull String itemId, @Nonnull String key) {
        return "items." + itemId + "." + key;
    }

    @Nonnull
    public static String configId(@Nonnull String specId) {
        return specId.startsWith("WWSF_") ? specId : "WWSF_" + specId;
    }
}
