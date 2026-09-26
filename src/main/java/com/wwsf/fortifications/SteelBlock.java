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
 * Full steel construction block. The red-nether-brick family is globally presented as steel by
 * the combined resource pack, giving the block matching slab, wall, and stair forms.
 */
public final class SteelBlock extends WWSFItem {

    public static final String ID = "WWSF_STEEL_BLOCK";

    public SteelBlock(@Nonnull ItemGroup category) {
        super(
            category,
            new SlimefunItemStack(
                ID,
                Material.RED_NETHER_BRICKS,
                "&7Steel Block",
                "&8Fortification material",
                "",
                "&7A solid steel construction block.",
                "&7Can be shaped into slabs, walls, and stairs."
            ),
            RecipeType.ENHANCED_CRAFTING_TABLE,
            createRecipe()
        );
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT,
            SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT,
            SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT
        };
    }

    @Nonnull
    public static SlimefunItemStack recipeIngredient() {
        return new SlimefunItemStack(ID, Material.RED_NETHER_BRICKS, "&7Steel Block");
    }

    public static boolean isSteelMaterial(@Nonnull Material material) {
        return material == Material.RED_NETHER_BRICKS
            || material == Material.RED_NETHER_BRICK_SLAB
            || material == Material.RED_NETHER_BRICK_WALL
            || material == Material.RED_NETHER_BRICK_STAIRS;
    }

    public static void registerStatic(WWSFPlugin plugin) {
        new SteelBlock(FortificationsCategory.FORTIFICATIONS).register(plugin);
    }
}
