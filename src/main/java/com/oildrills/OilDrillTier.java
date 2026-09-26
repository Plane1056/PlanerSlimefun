package com.oildrills;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public enum OilDrillTier {
    MARK_I("mark-1", "OIL_DRILL_MARK_I", "§7§lField Drill §8[Mark I]"),
    MARK_II("mark-2", "OIL_DRILL_MARK_II", "§f§lField Drill §7[Mark II]"),
    MARK_III("mark-3", "OIL_DRILL_MARK_III", "§6§lField Drill §e[Mark III]");

    private final String configKey;
    private final String itemId;
    private final String displayPrefix;

    OilDrillTier(String configKey, String itemId, String displayPrefix) {
        this.configKey = configKey;
        this.itemId = itemId;
        this.displayPrefix = displayPrefix;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayPrefix() {
        return displayPrefix;
    }

    public Material getCoreMaterial() {
        return switch (this) {
            case MARK_I -> Material.PISTON;
            case MARK_II -> Material.STICKY_PISTON;
            case MARK_III -> Material.OBSERVER;
        };
    }

    public static OilDrillTier fromCoreMaterial(Material material) {
        return switch (material) {
            case PISTON -> MARK_I;
            case STICKY_PISTON -> MARK_II;
            case OBSERVER -> MARK_III;
            default -> null;
        };
    }

    public int getSeconds(FileConfiguration cfg) {
        return Math.max(1, cfg.getInt("drills." + configKey + ".seconds", defaultSeconds()));
    }

    public int getCoalPerBarrel(FileConfiguration cfg) {
        return cfg.getInt("drills." + configKey + ".coal-per-barrel", defaultCoal());
    }

    public long rollDelayMs(FileConfiguration cfg) {
        return getSeconds(cfg) * 1000L;
    }

    public static OilDrillTier fromSlimefunItem(SlimefunItem item) {
        if (!(item instanceof OilDrillItem drill)) {
            return null;
        }
        return drill.getTier();
    }

    public ItemStack createDrillItem() {
        OilDrillItem item = switch (this) {
            case MARK_I -> OilItems.DRILL_MARK_I;
            case MARK_II -> OilItems.DRILL_MARK_II;
            case MARK_III -> OilItems.DRILL_MARK_III;
        };
        return item.getItem().clone();
    }

    private int defaultSeconds() {
        return switch (this) {
            case MARK_I -> 60;
            case MARK_II -> 18;
            case MARK_III -> 9;
        };
    }

    private int defaultCoal() {
        return switch (this) {
            case MARK_I -> 10;
            case MARK_II -> 8;
            case MARK_III -> 5;
        };
    }
}
