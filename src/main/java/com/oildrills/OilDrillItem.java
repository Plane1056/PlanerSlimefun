package com.oildrills;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import com.wwsf.fortifications.SteelBlock;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

public class OilDrillItem extends SimpleSlimefunItem<BlockUseHandler> {

    private final OilDrillTier tier;

    public OilDrillItem(ItemGroup group, SlimefunItemStack stack, RecipeType type, ItemStack[] recipe, OilDrillTier tier) {
        super(group, stack, type, recipe);
        this.tier = tier;
    }

    public OilDrillTier getTier() {
        return tier;
    }

    @Override
    public BlockUseHandler getItemHandler() {
        return (PlayerRightClickEvent event) -> {
            Block block = event.getClickedBlock().orElse(null);
            if (block == null) {
                return;
            }

            DrillManager manager = OilDrillsPlugin.getInstance().getDrillManager();
            if (manager != null) {
                manager.openControlPanel(event.getPlayer(), block.getLocation());
                event.cancel();
            }
        };
    }

    static ItemStack[] recipeMarkI() {
        return new ItemStack[] {
            new ItemStack(org.bukkit.Material.IRON_BLOCK), new ItemStack(org.bukkit.Material.REDSTONE_BLOCK), new ItemStack(org.bukkit.Material.IRON_BLOCK),
            new ItemStack(org.bukkit.Material.IRON_INGOT), new ItemStack(org.bukkit.Material.PISTON), new ItemStack(org.bukkit.Material.IRON_INGOT),
            new ItemStack(org.bukkit.Material.IRON_BLOCK), new ItemStack(org.bukkit.Material.DISPENSER), new ItemStack(org.bukkit.Material.IRON_BLOCK)
        };
    }

    static ItemStack[] recipeMarkII(ItemStack markIDrill) {
        return new ItemStack[] {
            SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT, SlimefunItems.STEEL_INGOT,
            SlimefunItems.STEEL_INGOT, markIDrill, SlimefunItems.STEEL_INGOT,
            new ItemStack(org.bukkit.Material.REDSTONE_BLOCK), SlimefunItems.ELECTRIC_MOTOR, new ItemStack(org.bukkit.Material.REDSTONE_BLOCK)
        };
    }

    static ItemStack[] recipeMarkIII(ItemStack markIIDrill) {
        return new ItemStack[] {
            SteelBlock.recipeIngredient(), SlimefunItems.REDSTONE_ALLOY, SteelBlock.recipeIngredient(),
            SlimefunItems.REDSTONE_ALLOY, markIIDrill, SlimefunItems.REDSTONE_ALLOY,
            SlimefunItems.ELECTRIC_MOTOR, SlimefunItems.HEATING_COIL, SlimefunItems.ELECTRIC_MOTOR
        };
    }
}
