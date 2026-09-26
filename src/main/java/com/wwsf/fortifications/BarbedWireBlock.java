package com.wwsf.fortifications;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.WWSFItem;
import com.wwsf.setup.FortificationsCategory;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

/**
 * Slimefun recipe wrapper for HowitzerArtillery's authoritative barbed-wire coil.
 */
public class BarbedWireBlock extends WWSFItem {

    public static final String ID = "WWSF_BARBED_WIRE";

    private BarbedWireBlock(@Nonnull ItemGroup category, @Nonnull ItemStack howitzerCoil) {
        super(
            category,
            new SlimefunItemStack(ID, howitzerCoil),
            RecipeType.ENHANCED_CRAFTING_TABLE,
            createRecipe()
        );
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT,
            SlimefunItems.STEEL_INGOT, new ItemStack(Material.STICK), SlimefunItems.STEEL_INGOT,
            SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT
        };
    }

    public static void registerStatic(WWSFPlugin plugin) {
        plugin.getHowitzerBridge().createBarbedWireCoil().ifPresentOrElse(
            coil -> {
                new BarbedWireBlock(FortificationsCategory.FORTIFICATIONS, coil).register(plugin);
                plugin.getLogger().info(
                    "Registered the Slimefun barbed-wire recipe with Howitzer's real coil output."
                );
            },
            () -> plugin.getLogger().warning(
                "HowitzerArtillery is unavailable; the WWSF barbed-wire recipe was not registered."
            )
        );
    }
}
