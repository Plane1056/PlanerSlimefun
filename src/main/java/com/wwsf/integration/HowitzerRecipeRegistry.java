package com.wwsf.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.oildrills.OilItems;
import com.wwsf.WWSFPlugin;
import com.wwsf.setup.MachinesCategory;
import com.wwsf.setup.WWSFCategory;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

/**
 * Publishes HowitzerArtillery's survival items as WWSF Slimefun recipes.
 *
 * <p>WWSF2 owns the {@code wwsf} and {@code great_war_industries} item groups,
 * so the recipes are registered here rather than inside the standalone
 * artillery plugin. Every output is built by {@link HowitzerArtilleryBridge},
 * so the crafted item carries Howitzer's own {@code arty:item_id} tag, model
 * and lore. When HowitzerArtillery is absent nothing is registered and its
 * vanilla firing-cord recipe is left alone.</p>
 */
public final class HowitzerRecipeRegistry {

    private HowitzerRecipeRegistry() {
    }

    private record Entry(String slimefunId, String artilleryType, ItemGroup group, ItemStack[] recipe) {
    }

    public static void register(@Nonnull WWSFPlugin plugin) {
        HowitzerArtilleryBridge bridge = plugin.getHowitzerBridge();
        if (!bridge.isAvailable()) {
            plugin.getLogger().info(
                "HowitzerArtillery not present — its WWSF recipes were skipped.");
            return;
        }

        ItemStack steel = SlimefunItems.STEEL_INGOT;
        List<Entry> entries = List.of(
            new Entry("HOWITZER_LARGE_SHELL_CASING", "LARGE_SHELL_CASING",
                MachinesCategory.MACHINES, cauldronRecipe(SlimefunItems.BRASS_INGOT)),
            new Entry("HOWITZER_FIRE_SOLUTION_BOOK", "SOLUTION_BOOK",
                WWSFCategory.WWSF, plusRecipe(steel, new ItemStack(Material.BOOK))),
            new Entry("HOWITZER_RIP_CORD", "RIP_CORD",
                WWSFCategory.WWSF, plusRecipe(steel, new ItemStack(Material.FISHING_ROD))),
            new Entry("HOWITZER_RANGEFINDER_PERISCOPE", "RANGEFINDER",
                WWSFCategory.WWSF, plusRecipe(steel, new ItemStack(Material.SPYGLASS))),
            new Entry("HOWITZER_CHARGE_LOADING_PRESS", "CHARGE_PRESS",
                WWSFCategory.WWSF, plusRecipe(steel, new ItemStack(Material.CRAFTER))),
            new Entry("HOWITZER_CALCULATOR", "CALCULATOR",
                WWSFCategory.WWSF, plusRecipe(steel, SlimefunItems.ANDROID_MEMORY_CORE))
        );

        int registered = 0;
        for (Entry entry : entries) {
            if (registerItem(plugin, bridge, entry)) {
                registered++;
            }
        }

        registerGasDrum(plugin, bridge);

        if (registered > 0 && bridge.retireVanillaRecipes()) {
            plugin.getLogger().info(
                "Registered " + registered + " HowitzerArtillery recipes in the WWSF guide; "
                    + "the legacy vanilla firing-cord recipe was removed.");
        }
    }

    private static boolean registerItem(
        WWSFPlugin plugin,
        HowitzerArtilleryBridge bridge,
        Entry entry
    ) {
        if (entry.group() == null) {
            plugin.getLogger().warning(
                "WWSF item group missing — skipped " + entry.slimefunId() + ".");
            return false;
        }
        if (SlimefunItem.getOptionalById(entry.slimefunId()).isPresent()) {
            return false;
        }
        Optional<ItemStack> output = bridge.createItem(entry.artilleryType());
        if (output.isEmpty()) {
            plugin.getLogger().warning(
                "HowitzerArtillery could not supply " + entry.artilleryType()
                    + "; its recipe was skipped.");
            return false;
        }

        ItemStack howitzerItem = output.get();
        new NonDroppingHowitzerBlock(
            entry.group(),
            new SlimefunItemStack(entry.slimefunId(), howitzerItem),
            RecipeType.ENHANCED_CRAFTING_TABLE,
            entry.recipe(),
            howitzerItem
        ).register((SlimefunAddon) plugin);
        return true;
    }

    private static void registerGasDrum(WWSFPlugin plugin, HowitzerArtilleryBridge bridge) {
        if (MachinesCategory.MACHINES == null
            || SlimefunItem.getOptionalById("WWSF_GAS_RELEASE_DRUM").isPresent()) {
            return;
        }
        Optional<ItemStack> drum = bridge.createGasDrum();
        if (drum.isEmpty()) {
            plugin.getLogger().warning(
                "HowitzerArtillery gas drum was unavailable; its recipe was skipped.");
            return;
        }

        ItemStack gasDrum = drum.get();
        new NonDroppingHowitzerBlock(
            MachinesCategory.MACHINES,
            new SlimefunItemStack("WWSF_GAS_RELEASE_DRUM", gasDrum),
            RecipeType.ENHANCED_CRAFTING_TABLE,
            gasDrumRecipe(gasBottle()),
            gasDrum
        ).register((SlimefunAddon) plugin);
    }

    /**
     * A Slimefun wrapper whose drops are owned entirely by HowitzerArtillery.
     *
     * <p>The gas drum, charge press, large shell casing and calculator are all
     * placeable Howitzer blocks whose own break handlers call
     * {@code setDropItems(false)} and drop the authoritative item — including
     * per-block state such as a drum's remaining charge. Slimefun would
     * otherwise drop a second copy, which is the reported duplication bug.
     * Returning no drops here is harmless for the non-placeable items.</p>
     */
    private static final class NonDroppingHowitzerBlock extends SlimefunItem {
        private NonDroppingHowitzerBlock(
            ItemGroup group,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe,
            ItemStack recipeOutput
        ) {
            super(group, item, recipeType, recipe, recipeOutput);
        }

        @Override
        public Collection<ItemStack> getDrops() {
            return List.of();
        }

        @Override
        public Collection<ItemStack> getDrops(Player player) {
            return List.of();
        }
    }

    private static ItemStack gasBottle() {
        if (OilItems.GAS_BOTTLE != null) {
            return OilItems.GAS_BOTTLE.clone();
        }
        return new SlimefunItemStack("WWSF_GAS_BOTTLE", Material.POTION, "§f§lGas Bottle");
    }

    static final int GAS_DRUM_BARREL_SLOT = 4;
    static final int[] GAS_DRUM_BOTTLE_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};

    static ItemStack[] gasDrumRecipe(ItemStack gasBottle) {
        ItemStack[] recipe = new ItemStack[9];
        for (int slot : GAS_DRUM_BOTTLE_SLOTS) {
            recipe[slot] = gasBottle.clone();
        }
        // Potion inputs leave glass bottles behind. A consumed barrel in the centre
        // creates the free slot Slimefun needs for the finished Gas Drum output.
        recipe[GAS_DRUM_BARREL_SLOT] = new ItemStack(Material.BARREL);
        return recipe;
    }

    /** The cauldron shape leaves the top-centre and centre slots empty. */
    static final int[] CAULDRON_EMPTY_SLOTS = {1, 4};
    /** The plus shape fills only these four slots with steel. */
    static final int[] PLUS_ARM_SLOTS = {1, 3, 5, 7};
    /** The plus shape leaves the corners empty. */
    static final int[] PLUS_EMPTY_SLOTS = {0, 2, 6, 8};
    /** Centre slot shared by the plus recipes. */
    static final int PLUS_CENTRE_SLOT = 4;

    /** Seven ingots around an empty centre column — the casing shape. */
    static ItemStack[] cauldronRecipe(ItemStack ingredient) {
        ItemStack[] recipe = new ItemStack[9];
        for (int slot = 0; slot < recipe.length; slot++) {
            recipe[slot] = ingredient.clone();
        }
        for (int empty : CAULDRON_EMPTY_SLOTS) {
            recipe[empty] = null;
        }
        return recipe;
    }

    /** Four steel ingots in a plus around one centre ingredient. */
    static ItemStack[] plusRecipe(ItemStack steel, ItemStack centre) {
        ItemStack[] recipe = new ItemStack[9];
        for (int arm : PLUS_ARM_SLOTS) {
            recipe[arm] = steel.clone();
        }
        recipe[PLUS_CENTRE_SLOT] = centre.clone();
        return recipe;
    }
}
