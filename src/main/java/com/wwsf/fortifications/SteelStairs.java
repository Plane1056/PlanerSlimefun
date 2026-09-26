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

public final class SteelStairs extends WWSFItem {

    public static final String ID = "WWSF_STEEL_STAIRS";

    public SteelStairs(@Nonnull ItemGroup category) {
        super(
            category,
            new SlimefunItemStack(
                ID,
                Material.RED_NETHER_BRICK_STAIRS,
                "&7Steel Stairs",
                "&8Fortification material",
                "",
                "&7A stepped steel construction piece."
            ),
            RecipeType.ENHANCED_CRAFTING_TABLE,
            createRecipe()
        );
    }

    @Override
    public void postRegister() {
        super.postRegister();
        ItemStack output = getItem().clone();
        output.setAmount(5);
        setRecipeOutput(output);
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            SteelBlock.recipeIngredient(), null, null,
            SteelBlock.recipeIngredient(), SteelBlock.recipeIngredient(), null,
            SteelBlock.recipeIngredient(), SteelBlock.recipeIngredient(), SteelBlock.recipeIngredient()
        };
    }

    public static void registerStatic(WWSFPlugin plugin) {
        new SteelStairs(FortificationsCategory.FORTIFICATIONS).register(plugin);
    }
}
