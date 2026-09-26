package com.wwsf.gas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import com.wwsf.WWSFPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * Stores a list of base64-encoded items that grant immunity to chlorine gas.
 * Saved to {@code Gas_Immunity.yml} in the plugin data folder.
 */
public final class GasImmunityConfig {

    private static final String FILE_NAME = "Gas_Immunity.yml";
    private static final List<String> immunityItems = new ArrayList<>();
    private static boolean loaded = false;

    private GasImmunityConfig() {
    }

    /**
     * Loads immunity items from the config file.
     */
    public static void load() {
        immunityItems.clear();
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            loaded = true;
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> items = config.getStringList("immunity-items");
        for (String base64 : items) {
            if (base64 != null && !base64.isEmpty()) {
                immunityItems.add(base64);
            }
        }
        plugin.getLogger().info("Loaded " + immunityItems.size() + " gas immunity items.");
        loaded = true;
    }

    /**
     * Saves the current immunity items to the config file.
     */
    public static void save() {
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        YamlConfiguration config = new YamlConfiguration();
        config.set("immunity-items", new ArrayList<>(immunityItems));
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save gas immunity config", e);
        }
    }

    /**
     * Adds a base64-encoded item to the immunity list.
     */
    public static void addItem(@Nonnull String base64) {
        if (!immunityItems.contains(base64)) {
            immunityItems.add(base64);
        }
    }

    /**
     * Removes a base64-encoded item from the immunity list.
     */
    public static void removeItem(@Nonnull String base64) {
        immunityItems.remove(base64);
    }

    /**
     * @return unmodifiable list of all registered immunity item base64 strings
     */
    @Nonnull
    public static List<String> getImmunityItems() {
        return java.util.Collections.unmodifiableList(immunityItems);
    }

    /**
     * Checks if an ItemStack matches any registered immunity item.
     * Compares material + display name + lore, not exact base64,
     * to avoid issues with durability, enchantments, or other NBT changes.
     */
    public static boolean isImmunityItem(@Nonnull ItemStack item) {
        if (!loaded) load();
        if (immunityItems.isEmpty()) return false;

        for (String storedBase64 : immunityItems) {
            if (itemsMatch(item, storedBase64)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares an ItemStack against a stored base64 template by checking
     * Slimefun ID (most reliable), then material + display name + lore.
     */
    private static boolean itemsMatch(@Nonnull ItemStack item, @Nonnull String storedBase64) {
        ItemStack template = deserializeItem(storedBase64);
        if (template == null) return false;

        // Material check (fast fail)
        if (item.getType() != template.getType()) return false;

        // Slimefun ID comparison — if both items are Slimefun items, compare IDs
        SlimefunItem templateSfItem = SlimefunItem.getByItem(template);
        SlimefunItem itemSfItem = SlimefunItem.getByItem(item);
        if (templateSfItem != null && itemSfItem != null) {
            return templateSfItem.getId().equals(itemSfItem.getId());
        }

        // Compare display name + lore
        org.bukkit.inventory.meta.ItemMeta templateMeta = template.getItemMeta();
        org.bukkit.inventory.meta.ItemMeta itemMeta = item.getItemMeta();

        if (templateMeta == null && itemMeta == null) return true;
        if (templateMeta == null || itemMeta == null) return false;

        String templateName = templateMeta.hasDisplayName() ? templateMeta.getDisplayName() : "";
        String itemName = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : "";
        if (!templateName.equals(itemName)) return false;

        java.util.List<String> templateLore = templateMeta.hasLore() ? templateMeta.getLore() : java.util.Collections.emptyList();
        java.util.List<String> itemLore = itemMeta.hasLore() ? itemMeta.getLore() : java.util.Collections.emptyList();
        return java.util.Objects.equals(templateLore, itemLore);
    }

    /**
     * Serializes an ItemStack to a base64 string.
     */
    @Nonnull
    public static String serializeItem(@Nonnull ItemStack item) {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            try (BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
                oos.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            return item.getType().name();
        }
    }

    /**
     * Deserializes a base64 string back to an ItemStack.
     */
    public static ItemStack deserializeItem(@Nonnull String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            try (BukkitObjectInputStream ois = new BukkitObjectInputStream(new java.io.ByteArrayInputStream(bytes))) {
                return (ItemStack) ois.readObject();
            }
        } catch (Exception e) {
            return null;
        }
    }
}