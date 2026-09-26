package com.wwsf.ammo;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wwsf.artillery.BlastProfile;
import com.wwsf.artillery.ShellType;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;

public class AmmoItem extends SlimefunItem {

    private static final int[] CUSTOM_MODEL_DATA = {
        10001, // STANDARD - FIREWORK_STAR
        10003, // INCENDIARY - BLAZE_POWDER (index 1)
        10002, // SHRAPNEL - ARROW (index 2)
        10004, // ARMOR_PIERCING - FIREWORK_STAR
        10005, // CLUSTER - FIREWORK_ROCKET
        10006  // SMOKE - DRAGON_BREATH
    };

    private final ShellType shellType;
    private final int energyCost;
    private final int modelData;

    public AmmoItem(@Nonnull ItemGroup category, @Nonnull String id, @Nonnull Material material, @Nonnull String name, @Nonnull ShellType type, int energyCost, ItemStack[] recipe) {
        this(category, id, material, name, new String[] { ChatColor.GRAY + "Type: " + ChatColor.WHITE + type.name() }, type, energyCost, recipe);
    }

    public AmmoItem(@Nonnull ItemGroup category, @Nonnull String id, @Nonnull Material material, @Nonnull String name, @Nonnull String[] lore, @Nonnull ShellType type, int energyCost, ItemStack[] recipe) {
        super(category, createStack(id, material, name, lore, modelDataFor(type)), RecipeType.ENHANCED_CRAFTING_TABLE, recipe);
        this.shellType = type;
        this.energyCost = energyCost;
        this.modelData = modelDataFor(type);
    }

    @Nonnull
    private static int modelDataFor(@Nonnull ShellType type) {
        return CUSTOM_MODEL_DATA[type.ordinal()];
    }

    @Nonnull
    private static SlimefunItemStack createStack(@Nonnull String id, @Nonnull Material material, @Nonnull String name, @Nonnull String[] lore, int modelData) {
        SlimefunItemStack stack = new SlimefunItemStack(id, material, name, meta -> {
            applyCustomModelData(meta, modelData);
            meta.setLore(java.util.Arrays.asList(lore));
        });
        // Bake custom model data directly into the ItemStack's meta so it survives
        // cloning, serialization, and recipe output creation.
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            applyCustomModelData(meta, modelData);
            meta.setLore(java.util.Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static void applyCustomModelData(@Nonnull ItemMeta meta, int modelData) {
        com.wwsf.util.CustomModelDataUtil.apply(meta, modelData);
    }

    @Override
    public void postRegister() {
        super.postRegister();
        setRecipeOutput(getItem());
    }

    @Override
    public @Nonnull ItemStack getItem() {
        ItemStack item = super.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            applyCustomModelData(meta, modelData);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Nonnull
    public ShellType getShellType() {
        return shellType;
    }

    public int getEnergyCost() {
        return energyCost;
    }
}
