package com.oildrills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;

/**
 * Cross-machine and vanilla recipes used by the oil production chain.
 */
final class OilRecipeRegistry {

    // This Slimefun-United build advances AContainer operations every 10 server ticks.
    private static final int GAS_BOTTLE_PROCESS_TICKS = 30 * 2;

    private static NamespacedKey crudeOilToSulfateKey;
    private static NamespacedKey gunpowderKey;
    private static SlimefunItem oilBucketItem;
    private static RecipeType originalOilBucketRecipeType;
    private static ItemStack[] originalOilBucketRecipe;
    private static final List<RegisteredMachineRecipe> GAS_BOTTLE_RECIPES = new ArrayList<>();

    private OilRecipeRegistry() {
    }

    static void register(JavaPlugin plugin) {
        registerCrudeOilBlasting(plugin);
        registerGunpowderCrafting(plugin);
        registerGasBottleProcessing(plugin);
        replaceOilBucketGuideRecipe(plugin);
    }

    static void unregister() {
        if (crudeOilToSulfateKey != null) {
            Bukkit.removeRecipe(crudeOilToSulfateKey);
            crudeOilToSulfateKey = null;
        }
        if (gunpowderKey != null) {
            Bukkit.removeRecipe(gunpowderKey);
            gunpowderKey = null;
        }
        for (RegisteredMachineRecipe registered : GAS_BOTTLE_RECIPES) {
            registered.machine().getMachineRecipes().remove(registered.recipe());
        }
        GAS_BOTTLE_RECIPES.clear();
        restoreOilBucketGuideRecipe();
    }

    private static void registerCrudeOilBlasting(JavaPlugin plugin) {
        crudeOilToSulfateKey = new NamespacedKey(plugin, "crude_oil_to_sulfate");
        ItemStack input = OilItems.CRUDE_OIL.clone();
        input.setAmount(1);
        ItemStack output = SlimefunItems.SULFATE.clone();
        output.setAmount(1);
        BlastingRecipe recipe = new BlastingRecipe(
                crudeOilToSulfateKey,
                output,
                new RecipeChoice.ExactChoice(input),
                0.0F,
                200
        );
        Bukkit.addRecipe(recipe);
    }

    private static void registerGunpowderCrafting(JavaPlugin plugin) {
        gunpowderKey = new NamespacedKey(plugin, "sulfate_charcoal_gunpowder");
        ItemStack sulfate = SlimefunItems.SULFATE.clone();
        sulfate.setAmount(1);
        ShapelessRecipe recipe = new ShapelessRecipe(
                gunpowderKey,
                new ItemStack(Material.GUNPOWDER)
        );
        recipe.addIngredient(new RecipeChoice.ExactChoice(sulfate));
        recipe.addIngredient(Material.CHARCOAL);
        Bukkit.addRecipe(recipe);
    }

    private static void registerGasBottleProcessing(JavaPlugin plugin) {
        registerGasBottleProcessingFor("HEATED_PRESSURE_CHAMBER");
        registerGasBottleProcessingFor("HEATED_PRESSURE_CHAMBER_2");
        if (GAS_BOTTLE_RECIPES.isEmpty()) {
            plugin.getLogger().warning(
                    "Heated Pressure Chamber was not available; the Fuel Bucket to Gas Bottle "
                            + "recipe could not be installed."
            );
        }
    }

    private static void registerGasBottleProcessingFor(String machineId) {
        SlimefunItem machine = SlimefunItem.getById(machineId);
        if (!(machine instanceof AContainer container)) {
            return;
        }

        MachineRecipe recipe = new MachineRecipe(
                GAS_BOTTLE_PROCESS_TICKS * container.getSpeed(),
                new ItemStack[] { SlimefunItems.FUEL_BUCKET.clone() },
                new ItemStack[] { OilItems.GAS_BOTTLE.clone() }
        );
        container.registerRecipe(recipe);
        GAS_BOTTLE_RECIPES.add(new RegisteredMachineRecipe(container, recipe));

        if (OilItems.LEGACY_FUEL_BUCKET != null) {
            MachineRecipe legacyRecipe = new MachineRecipe(
                    GAS_BOTTLE_PROCESS_TICKS * container.getSpeed(),
                    new ItemStack[] { OilItems.LEGACY_FUEL_BUCKET.clone() },
                    new ItemStack[] { OilItems.GAS_BOTTLE.clone() }
            );
            container.registerRecipe(legacyRecipe);
            GAS_BOTTLE_RECIPES.add(new RegisteredMachineRecipe(container, legacyRecipe));
        }
    }

    private static void replaceOilBucketGuideRecipe(JavaPlugin plugin) {
        SlimefunItem item = SlimefunItem.getById("BUCKET_OF_OIL");
        if (item == null) {
            plugin.getLogger().warning(
                    "Slimefun Bucket of Oil was not available; its guide recipe could not be replaced."
            );
            return;
        }

        oilBucketItem = item;
        originalOilBucketRecipeType = item.getRecipeType();
        originalOilBucketRecipe = item.getRecipe();

        RecipeType refineryRecipeType = new RecipeType(
                new NamespacedKey(plugin, "oil_refinery_process"),
                OilItems.OIL_REFINERY,
                "§eOil Refinery"
        );
        ItemStack crudeOil = OilItems.CRUDE_OIL.clone();
        crudeOil.setAmount(9);
        ItemStack[] guideRecipe = new ItemStack[9];
        guideRecipe[4] = crudeOil;
        item.setRecipeType(refineryRecipeType);
        item.setRecipe(guideRecipe);
    }

    private static void restoreOilBucketGuideRecipe() {
        if (oilBucketItem == null) {
            return;
        }
        oilBucketItem.setRecipeType(originalOilBucketRecipeType);
        oilBucketItem.setRecipe(originalOilBucketRecipe);
        oilBucketItem = null;
        originalOilBucketRecipeType = null;
        originalOilBucketRecipe = null;
    }

    private record RegisteredMachineRecipe(AContainer machine, MachineRecipe recipe) {
    }
}
