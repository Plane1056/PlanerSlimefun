package com.wwsf.ammo;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wwsf.WWSFPlugin;
import com.wwsf.items.ShellCasing;
import com.wwsf.setup.ElectricityCategory;
import com.wwsf.util.CustomModelDataUtil;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;

public class MachineGunAmmoItem extends SlimefunItem {

    private static final int MODEL_DATA = 10007;

    public MachineGunAmmoItem(@Nonnull WWSFPlugin plugin) {
        super(
            ElectricityCategory.AMMUNITION,
            new SlimefunItemStack("WWSF_MACHINE_GUN_AMMO", Material.FIREWORK_STAR, "&fMachine Gun Ammo", meta -> {
                applyCustomModelData(meta, MODEL_DATA);
                meta.setLore(java.util.List.of("&7Rapid-fire cartridge for the Maxim Machine Gun."));
            }),
            RecipeType.ENHANCED_CRAFTING_TABLE,
            new ItemStack[] {
                new SlimefunItemStack(ShellCasing.ID, Material.BLAZE_ROD, "&fShell Casing"),
                new ItemStack(Material.IRON_NUGGET), new ItemStack(Material.IRON_NUGGET),
                new ItemStack(Material.GUNPOWDER), null, null,
                null, null, null
            }
        );
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
            applyCustomModelData(meta, MODEL_DATA);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void applyCustomModelData(@Nonnull ItemMeta meta, int modelData) {
        com.wwsf.util.CustomModelDataUtil.apply(meta, modelData);
    }
}
