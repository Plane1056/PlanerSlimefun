package com.wwsf.config;

import javax.annotation.Nonnull;

import org.bukkit.configuration.file.FileConfiguration;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * Base Slimefun item with WWSF per-item config access via {@link #getItemConfig()}.
 */
public abstract class WWSFItem extends SlimefunItem {

    protected WWSFItem(
        @Nonnull io.github.thebusybiscuit.slimefun4.api.items.ItemGroup category,
        @Nonnull io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack item,
        @Nonnull io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType recipeType,
        org.bukkit.inventory.ItemStack[] recipe
    ) {
        super(category, item, recipeType, recipe);
    }

    @Nonnull
    public FileConfiguration getItemConfig() {
        return ItemConfigHelper.getItemConfig(this);
    }
}
