package com.oildrills;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

/**
 * Single-use tool for detecting an oil field within 50 blocks.
 *
 * <p>Interaction is handled by {@link DowsingRodListener} rather than Slimefun's item-use
 * callback so right-clicking ordinary ground is handled consistently.</p>
 */
public final class DowsingRodItem extends SlimefunItem {

    public static final String ID = "WWSF_DOWSING_ROD";

    public DowsingRodItem(ItemGroup group) {
        super(group, createStack(), RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    private static SlimefunItemStack createStack() {
        return new SlimefunItemStack(
                ID,
                Material.BLAZE_ROD,
                "§6§lDowsing Rod",
                "§7Right-click the ground to check",
                "§7for an oil field within ±50 blocks.",
                "§cBreaks after one use."
        );
    }

    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            null, SlimefunItems.STEEL_INGOT, null,
            null, SlimefunItems.STEEL_INGOT, null,
            null, SlimefunItems.STEEL_INGOT, null
        };
    }
}
