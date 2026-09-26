package com.wwsf.items;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wwsf.config.WWSFItem;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

/**
 * A basic crafting component used in gun recipes.
 */
public class GunPart extends WWSFItem {

    public static final String ID = "GUN_PART";

    public GunPart(@Nonnull ItemGroup category) {
        super(category, createStack(), RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    @Nonnull
    private static SlimefunItemStack createStack() {
        SlimefunItemStack stack = new SlimefunItemStack(ID, Material.PRISMARINE_CRYSTALS,
            ChatColor.WHITE + "Gun Part",
            meta -> {
                meta.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "A basic component for crafting firearms."
                ));
                com.wwsf.util.CustomModelDataUtil.apply(meta, 1);
            });
        // Bake custom model data directly into the ItemStack's meta so it survives
        // cloning, serialization, and recipe output creation.
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @Override
    public @Nonnull ItemStack getItem() {
        ItemStack item = super.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT, new ItemStack(Material.IRON_INGOT),
            new ItemStack(Material.REDSTONE), null, null,
            null, null, null
        };
    }

    @Override
    public void postRegister() {
        super.postRegister();
        setRecipeOutput(getItem());
    }
}
