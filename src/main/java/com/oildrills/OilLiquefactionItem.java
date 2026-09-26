package com.oildrills;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

/**
 * Electric coal-liquefaction plant: one full stack of coal becomes one crude oil.
 */
public final class OilLiquefactionItem extends AContainer implements FilteredInventoryBlock {

    private static final int[] INPUT_SLOTS = {10, 11, 12, 19, 20, 21};
    private static final int[] OUTPUT_SLOTS = {15, 16, 23, 24, 25};
    private static final int[] NO_SLOTS = {};
    private static final int[] BACKGROUND_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 17, 18, 22, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35
    };

    public OilLiquefactionItem(
            ItemGroup group,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe
    ) {
        super(group, item, recipeType, recipe);
        setCapacity(10_000);
        setEnergyConsumption(200);
        setProcessingSpeed(1);
        registerRecipe(
                // This Slimefun-United build advances AContainer operations every 10 server ticks.
                60 * 2,
                new ItemStack(Material.COAL, 64),
                OilItems.CRUDE_OIL
        );
    }

    @Override
    protected void constructMenu(BlockMenuPreset preset) {
        preset.setSize(36);
        preset.drawBackground(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), BACKGROUND_SLOTS);
        for (int slot : OUTPUT_SLOTS) {
            preset.addMenuClickHandler(slot, ChestMenuUtils.getDefaultOutputHandler());
        }
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.FIRE_CHARGE);
    }

    @Override
    public String getMachineIdentifier() {
        return "OIL_LIQUEFACTION_PLANT";
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
        return item != null && item.getType() == Material.COAL ? INPUT_SLOTS : NO_SLOTS;
    }

    @Override
    public BlockBreakHandler onBlockBreak() {
        return new BlockBreakHandler(true, true) {
            @Override
            public void onPlayerBreak(BlockBreakEvent event, ItemStack tool, List<ItemStack> drops) {
                drops.clear();
                BlockMenu menu = BlockStorage.getInventory(event.getBlock());
                if (menu == null) {
                    return;
                }
                addMachineContents(menu, INPUT_SLOTS, drops);
                addMachineContents(menu, OUTPUT_SLOTS, drops);
            }
        };
    }

    private static void addMachineContents(BlockMenu menu, int[] slots, List<ItemStack> drops) {
        for (int slot : slots) {
            ItemStack stack = menu.getItemInSlot(slot);
            if (stack != null
                    && !stack.isEmpty()
                    && stack.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                drops.add(stack);
            }
        }
    }
}
