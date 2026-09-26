package com.wwsf.items;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.ItemConfigHelper;
import com.wwsf.config.WWSFItem;
import com.wwsf.setup.WWSFCategory;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

/**
 * TEMPLATE: Copy this file to create new non-artillery items/blocks.
 * 
 * This example shows:
 * 1. A simple Slimefun item (extends WWSFItem)
 * 2. Config-driven stats (read from config.yml)
 * 3. Right-click behavior (ItemUseHandler)
 * 4. Static registration helper
 * 
 * To add a new item:
 * 1. Copy this file to a new name in the appropriate package
 * 2. Change ID, MATERIAL, DISPLAY_NAME, LORE, recipe
 * 3. Add any config keys to src/main/resources/config.yml
 * 4. Register it in the category's register() method
 * 
 * For blocks that place in the world, extend AbstractArtilleryBlock or
 * use SlimefunItem directly with a BlockPlaceHandler.
 */
public class ExampleItem extends WWSFItem {

    // ==================== STATIC CONSTANTS ====================
    // Change these for your new item
    public static final String ID = "WWSF_EXAMPLE_ITEM";
    public static final Material MATERIAL = Material.DIAMOND;
    public static final String DISPLAY_NAME = "&bExample Item";
    public static final String[] LORE = {
        "&8Template item — copy and modify",
        "",
        "&7This is an example non-artillery item.",
        "&7Right-click to activate."
    };

    // ==================== CONSTRUCTOR ====================
    public ExampleItem(@Nonnull ItemGroup category) {
        super(category, createItemStack(), RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    // ==================== ITEM STACK CREATION ====================
    private static SlimefunItemStack createItemStack() {
        return new SlimefunItemStack(ID, MATERIAL, DISPLAY_NAME, LORE);
    }

    // ==================== RECIPE ====================
    // 3x3 crafting grid (row-major: top-left to bottom-right)
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            SlimefunItems.STEEL_INGOT, new ItemStack(Material.DIAMOND), SlimefunItems.STEEL_INGOT,
            new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND),
            SlimefunItems.STEEL_INGOT, new ItemStack(Material.DIAMOND), SlimefunItems.STEEL_INGOT
        };
    }

    // ==================== HANDLERS ====================
    @Override
    public void preRegister() {
        // Register event handlers here (right-click, left-click, etc.)
        addItemHandler(onRightClick());
    }

    private ItemUseHandler onRightClick() {
        return (PlayerRightClickEvent event) -> {
            // Cancel the event to prevent vanilla behavior
            event.cancel();
            
            // Your custom logic here
            // Example: give the player a effect, spawn particles, etc.
        };
    }

    // ==================== CONFIG-DRIVEN STATS ====================
    // These read from config.yml under items.WWSF_EXAMPLE_ITEM.<key>
    // Add defaults here, override in config.yml
    
    public double getEffectRadius() {
        return ItemConfigHelper.getDouble(this, "example.radius", 5.0);
    }

    public int getEffectDuration() {
        return ItemConfigHelper.getInt(this, "example.duration", 100);
    }

    public float getPower() {
        return (float) ItemConfigHelper.getDouble(this, "example.power", 1.5);
    }

    // ==================== REGISTRATION ====================
    public static void registerStatic(WWSFPlugin plugin) {
        // Call this from your category's register() method
        new ExampleItem(WWSFCategory.WWSF).register(plugin);
    }
}