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

public final class SteelWall extends WWSFItem {

    public static final String ID = "WWSF_STEEL_WALL";

    public SteelWall(@Nonnull ItemGroup category) {
        super(
            category,
            new SlimefunItemStack(
                ID,
                Material.RED_NETHER_BRICK_WALL,
                "&7Steel Wall",
                "&8Fortification material",
                "",
                "&7A narrow steel defensive barrier."
            ),
            RecipeType.ENHANCED_CRAFTING_TABLE,
            createRecipe()
        );
    }

    @Override
    public void postRegister() {
        super.postRegister();
        ItemStack output = getItem().clone();
        output.setAmount(6);
        setRecipeOutput(output);
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            SteelBlock.recipeIngredient(), SteelBlock.recipeIngredient(), SteelBlock.recipeIngredient(),
            SteelBlock.recipeIngredient(), SteelBlock.recipeIngredient(), SteelBlock.recipeIngredient(),
            null, null, null
        };
    }

    public static void registerStatic(WWSFPlugin plugin) {
        new SteelWall(FortificationsCategory.FORTIFICATIONS).register(plugin);
    }
}
