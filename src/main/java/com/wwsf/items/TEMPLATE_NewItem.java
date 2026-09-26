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
 * TEMPLATE FOR EXTERNAL AI — COPY THIS FILE AND MODIFY
 * 
 * This is a complete, self-contained template for creating new non-artillery items.
 * An external AI can read this file and generate new items by following this exact pattern.
 * 
 * INSTRUCTIONS FOR AI:
 * 1. Change the class name (line 18) to match the item name
 * 2. Change ID (line 22) to unique identifier like "WWSF_MY_ITEM"
 * 3. Change MATERIAL (line 23) to desired block/item material
 * 4. Change DISPLAY_NAME (line 24) to item name with color codes
 * 5. Change LORE (line 25-30) to item description
 * 6. Update createRecipe() (line 56-64) with actual recipe
 * 7. Update onRightClick() (line 73-82) with actual behavior
 * 8. Add/remove config getters (line 88-100) as needed
 * 9. Register in WWSFCategory.register() method
 */
public class TEMPLATE_NewItem extends WWSFItem {

    // ==================== CONFIGURATION ====================
    // CHANGE THESE VALUES FOR YOUR ITEM
    
    public static final String ID = "WWSF_TEMPLATE_ITEM";  // Unique ID, e.g., "WWSF_MEDKIT"
    public static final Material MATERIAL = Material.DIAMOND;  // Item material
    public static final String DISPLAY_NAME = "&bTemplate Item";  // Name with color codes
    public static final String[] LORE = {
        "&8Template — copy and modify",
        "",
        "&7This is a template item.",
        "&7Right-click to use."
    };

    // ==================== CONSTRUCTOR ====================
    // DO NOT MODIFY unless you need different recipe type
    public TEMPLATE_NewItem(@Nonnull ItemGroup category) {
        super(category, createItemStack(), RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    // ==================== ITEM CREATION ====================
    // DO NOT MODIFY
    private static SlimefunItemStack createItemStack() {
        return new SlimefunItemStack(ID, MATERIAL, DISPLAY_NAME, LORE);
    }

    // ==================== RECIPE ====================
    // 3x3 CRAFTING GRID — modify this for your item
    // Row-major order: [0,1,2] = top row, [3,4,5] = middle, [6,7,8] = bottom
    // Use null for empty slots
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            SlimefunItems.STEEL_INGOT, new ItemStack(Material.DIAMOND), SlimefunItems.STEEL_INGOT,
            new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND),
            SlimefunItems.STEEL_INGOT, new ItemStack(Material.DIAMOND), SlimefunItems.STEEL_INGOT
        };
    }

    // ==================== EVENT HANDLERS ====================
    // Register handlers in preRegister()
    @Override
    public void preRegister() {
        addItemHandler(onRightClick());
    }

    // RIGHT-CLICK HANDLER — modify this for your item's behavior
    private ItemUseHandler onRightClick() {
        return (PlayerRightClickEvent event) -> {
            // Cancel vanilla behavior
            event.cancel();
            
            // Get the player
            // Player player = event.getPlayer();
            
            // YOUR CODE HERE:
            // Examples:
            // - player.heal(5);
            // - player.getWorld().spawnParticle(Particle.HEART, player.getLocation(), 10);
            // - player.sendMessage("&aYou used the item!");
        };
    }

    // ==================== CONFIG-DRIVEN STATS ====================
    // These read from config.yml under items.WWSF_TEMPLATE_ITEM.<key>
    // Add/remove these as needed for your item
    
    public double getEffectRadius() {
        return ItemConfigHelper.getDouble(this, "template.radius", 5.0);
    }

    public int getEffectDuration() {
        return ItemConfigHelper.getInt(this, "template.duration", 100);
    }

    public float getPower() {
        return (float) ItemConfigHelper.getDouble(this, "template.power", 1.5);
    }

    public long getCooldownMs() {
        return ItemConfigHelper.getLong(this, "template.cooldown-ms", 1000L);
    }

    // ==================== REGISTRATION ====================
    // Call this from your category's register() method
    public static void registerStatic(WWSFPlugin plugin) {
        new TEMPLATE_NewItem(WWSFCategory.WWSF).register(plugin);
    }
}