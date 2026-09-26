package com.wwsf.items;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.WWSFItem;
import com.wwsf.setup.ElectricityCategory;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

/**
 * Brass-and-steel cartridge casing used as the base for crafting artillery shells.
 */
public class ShellCasing extends WWSFItem {

    public static final String ID = "WWSF_SHELL_CASING";
    private static final int CUSTOM_MODEL_DATA = 10020;

    public ShellCasing(@Nonnull ItemGroup category) {
        super(category, createStack(), RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    @Override
    public void postRegister() {
        super.postRegister();
        ItemStack output = getItem();
        output.setAmount(3);
        setRecipeOutput(output);
    }

    @Override
    public @Nonnull ItemStack getItem() {
        ItemStack item = super.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Nonnull
    private static SlimefunItemStack createStack() {
        SlimefunItemStack stack = new SlimefunItemStack(ID, Material.BLAZE_ROD, "&fShell Casing", meta -> {
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            meta.setLore(java.util.List.of(
                "&7Brass and steel cartridge casing.",
                "&7Base component for crafting artillery shells."
            ));
        });
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            meta.setLore(java.util.List.of(
                "&7Brass and steel cartridge casing.",
                "&7Base component for crafting artillery shells."
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            SlimefunItems.BRASS_INGOT, SlimefunItems.BRASS_INGOT, SlimefunItems.STEEL_INGOT,
            null, null, null,
            null, null, null
        };
    }

    public static void registerStatic(WWSFPlugin plugin) {
        new ShellCasing(ElectricityCategory.AMMUNITION).register(plugin);
    }
}
