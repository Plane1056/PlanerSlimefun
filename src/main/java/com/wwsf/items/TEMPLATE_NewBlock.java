package com.wwsf.items;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.ItemConfigHelper;
import com.wwsf.config.WWSFItem;
import com.wwsf.setup.WWSFCategory;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * TEMPLATE FOR EXTERNAL AI — COPY THIS FILE AND MODIFY
 * 
 * This is a complete, self-contained template for creating new placeable blocks.
 * An external AI can read this file and generate new blocks by following this exact pattern.
 * 
 * INSTRUCTIONS FOR AI:
 * 1. Change the class name (line 18) to match the block name
 * 2. Change ID (line 22) to unique identifier like "WWSF_MY_BLOCK"
 * 3. Change MATERIAL (line 23) to desired block material
 * 4. Change DISPLAY_NAME (line 24) to block name with color codes
 * 5. Change LORE (line 25-32) to block description
 * 6. Update createRecipe() (line 58-66) with actual recipe
 * 7. Update onPlace() (line 75-88) with placement behavior
 * 8. Update onUse() (line 91-100) with right-click behavior
 * 9. Add/remove config getters (line 106-120) as needed
 * 10. Register in WWSFCategory.register() method
 */
public class TEMPLATE_NewBlock extends WWSFItem {

    // ==================== CONFIGURATION ====================
    // CHANGE THESE VALUES FOR YOUR BLOCK
    
    public static final String ID = "WWSF_TEMPLATE_BLOCK";  // Unique ID, e.g., "WWSF_MEDICAL_TENT"
    public static final Material MATERIAL = Material.STONE;  // Block material
    public static final String DISPLAY_NAME = "&7Template Block";  // Name with color codes
    public static final String[] LORE = {
        "&8Template — copy and modify",
        "",
        "&7This is a template block.",
        "&7Place it and right-click to use."
    };

    // ==================== CONSTRUCTOR ====================
    // DO NOT MODIFY unless you need different recipe type
    public TEMPLATE_NewBlock(@Nonnull ItemGroup category) {
        super(category, createItemStack(), RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    // ==================== ITEM CREATION ====================
    // DO NOT MODIFY
    private static SlimefunItemStack createItemStack() {
        return new SlimefunItemStack(ID, MATERIAL, DISPLAY_NAME, LORE);
    }

    // ==================== RECIPE ====================
    // 3x3 CRAFTING GRID — modify this for your block
    // Row-major order: [0,1,2] = top row, [3,4,5] = middle, [6,7,8] = bottom
    // Use null for empty slots
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.STONE), new ItemStack(Material.STONE), new ItemStack(Material.STONE),
            new ItemStack(Material.STONE), SlimefunItems.STEEL_PLATE, new ItemStack(Material.STONE),
            new ItemStack(Material.STONE), new ItemStack(Material.STONE), new ItemStack(Material.STONE)
        };
    }

    // ==================== EVENT HANDLERS ====================
    // Register handlers in preRegister()
    @Override
    public void preRegister() {
        addItemHandler(onPlace());
        addItemHandler(onUse());
    }

    // BLOCK PLACE HANDLER — called when player places the block
    private BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(@Nonnull org.bukkit.event.block.BlockPlaceEvent event) {
                // Store block owner
                WWSFPlugin plugin = WWSFPlugin.getInstance();
                BlockStorage.addBlockInfo(
                    event.getBlockPlaced(),
                    plugin.getOwnerKey().getKey(),
                    event.getPlayer().getUniqueId().toString()
                );
                
                // YOUR CODE HERE:
                // - Spawn entities (armor stands, etc.)
                // - Initialize block state
                // - Send messages
                event.getPlayer().sendMessage("&7Template block placed!");
            }
        };
    }

    // RIGHT-CLICK HANDLER — called when player right-clicks the placed block
    private ItemUseHandler onUse() {
        return (PlayerRightClickEvent event) -> {
            // Cancel vanilla behavior
            event.cancel();
            
            // Get the clicked block
            Block block = event.getClickedBlock().orElse(null);
            if (block == null) return;
            
            // Get the player
            // Player player = event.getPlayer();
            
            // YOUR CODE HERE:
            // Examples:
            // - player.getWorld().spawnParticle(Particle.HEART, block.getLocation().add(0.5, 1, 0.5), 10);
            // - player.sendMessage("&aYou activated the block!");
            // - player.heal(2);
        };
    }

    // ==================== CONFIG-DRIVEN STATS ====================
    // These read from config.yml under items.WWSF_TEMPLATE_BLOCK.<key>
    // Add/remove these as needed for your block
    
    public double getBlastResistance() {
        return ItemConfigHelper.getDouble(this, "template.blast-resistance", 6.0);
    }

    public int getEffectRadius() {
        return ItemConfigHelper.getInt(this, "template.radius", 5);
    }

    public long getCooldownMs() {
        return ItemConfigHelper.getLong(this, "template.cooldown-ms", 1000L);
    }

    public float getPower() {
        return (float) ItemConfigHelper.getDouble(this, "template.power", 1.5f);
    }

    // ==================== REGISTRATION ====================
    // Call this from your category's register() method
    public static void registerStatic(WWSFPlugin plugin) {
        new TEMPLATE_NewBlock(WWSFCategory.WWSF).register(plugin);
    }
}