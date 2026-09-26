package com.oildrills;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

public class FuelRefineryItem extends AContainer implements FilteredInventoryBlock {

    private static final int[] INPUT_SLOTS = {10, 11, 12, 19, 20, 21};
    private static final int[] OUTPUT_SLOTS = {15, 16, 23, 24, 25};
    private static final int[] NO_SLOTS = {};
    private static final int[] BACKGROUND_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 17, 18, 22, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35
    };

    public FuelRefineryItem(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(group, item, recipeType, recipe);
        setCapacity(10000);
        setEnergyConsumption(40);
        setProcessingSpeed(1);
        // This Slimefun-United build advances AContainer operations every 10 server ticks.
        registerRecipe(60 * 2,
            new ItemStack[] { SlimefunItems.OIL_BUCKET },
            new ItemStack[] { SlimefunItems.FUEL_BUCKET });
        OilDrillsPlugin.getInstance().getLogger().info("Fuel Refinery registered with ID: " + getMachineIdentifier() + ", capacity: 10000, consumption: 40 J/t");
    }

    @Override
    protected void constructMenu(BlockMenuPreset preset) {
        preset.setSize(36);
        // drawBackground marks slots as non-interactable background that won't drop on block break
        preset.drawBackground(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), BACKGROUND_SLOTS);
        for (int slot : OUTPUT_SLOTS) {
            preset.addMenuClickHandler(slot, ChestMenuUtils.getDefaultOutputHandler());
        }
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.BLAZE_POWDER);
    }

    @Override
    public String getMachineIdentifier() {
        return "FUEL_REFINERY";
    }

    @Override
    public int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    public int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }

    @Override
    public int[] getInputSlotsFor(ItemStack item) {
        if (item == null) {
            return NO_SLOTS;
        }
        if (SlimefunUtils.isItemSimilar(item, SlimefunItems.OIL_BUCKET, true)) {
            return INPUT_SLOTS;
        }
        return NO_SLOTS;
    }

    @Override
    public BlockBreakHandler onBlockBreak() {
        return new BlockBreakHandler(true, true) {
            @Override
            public void onPlayerBreak(BlockBreakEvent event, ItemStack tool, List<ItemStack> drops) {
                // Clear default drops (background glass, progress bar)
                drops.clear();
                // Only drop the actual input and output contents
                BlockMenu menu = BlockStorage.getInventory(event.getBlock());
                if (menu != null) {
                    for (int slot : INPUT_SLOTS) {
                        ItemStack stack = menu.getItemInSlot(slot);
                        if (stack != null && !stack.isEmpty() && !stack.getType().equals(Material.GRAY_STAINED_GLASS_PANE)) {
                            drops.add(stack);
                        }
                    }
                    for (int slot : OUTPUT_SLOTS) {
                        ItemStack stack = menu.getItemInSlot(slot);
                        if (stack != null && !stack.isEmpty() && !stack.getType().equals(Material.GRAY_STAINED_GLASS_PANE)) {
                            drops.add(stack);
                        }
                    }
                }
            }
        };
    }
}
