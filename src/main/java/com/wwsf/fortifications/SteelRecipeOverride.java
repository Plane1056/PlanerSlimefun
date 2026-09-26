package com.wwsf.fortifications;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

/**
 * Prevents vanilla recipes and stonecutting from creating untagged items that the combined
 * resource pack presents as steel. The displaced recipes are restored for clean plugin reloads.
 */
public final class SteelRecipeOverride {

    private static final Set<Material> STEEL_OUTPUTS = EnumSet.of(
        Material.RED_NETHER_BRICKS,
        Material.RED_NETHER_BRICK_SLAB,
        Material.RED_NETHER_BRICK_WALL,
        Material.RED_NETHER_BRICK_STAIRS
    );

    private static final Map<NamespacedKey, Recipe> DISPLACED_RECIPES = new LinkedHashMap<>();

    private SteelRecipeOverride() {
    }

    public static void install() {
        if (!DISPLACED_RECIPES.isEmpty()) {
            return;
        }

        Iterator<Recipe> recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (recipe instanceof Keyed keyed
                && "minecraft".equals(keyed.getKey().getNamespace())
                && STEEL_OUTPUTS.contains(recipe.getResult().getType())) {
                DISPLACED_RECIPES.put(keyed.getKey(), recipe);
            }
        }

        for (NamespacedKey key : DISPLACED_RECIPES.keySet()) {
            Bukkit.removeRecipe(key);
        }
    }

    public static void uninstall() {
        for (Map.Entry<NamespacedKey, Recipe> displaced : DISPLACED_RECIPES.entrySet()) {
            if (Bukkit.getRecipe(displaced.getKey()) == null) {
                Bukkit.addRecipe(displaced.getValue());
            }
        }

        DISPLACED_RECIPES.clear();
    }
}
