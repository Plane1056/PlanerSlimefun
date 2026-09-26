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
 * TEMPLATE: Copy this file to create new placeable blocks.
 * 
 * This example shows:
 * 1. A Slimefun block (extends WWSFItem)
 * 2. BlockPlaceHandler for when the block is placed
 * 3. ItemUseHandler for right-clicking the placed block
 * 4. Config-driven block stats
 * 5. Static registration helper
 * 
 * To add a new block:
 * 1. Copy this file to a new name in the appropriate package
 * 2. Change ID, MATERIAL, DISPLAY_NAME, LORE, recipe
 * 3. Add any config keys to src/main/resources/config.yml
 * 4. Register it in the category's register() method
 */
public class ExampleBlock extends WWSFItem {

    // ==================== STATIC CONSTANTS ====================
    public static final String ID = "WWSF_EXAMPLE_BLOCK";
    public static final Material MATERIAL = Material.STONE;
    public static final String DISPLAY_NAME = "&7Example Block";
    public static final String[] LORE = {
        "&8Template block — copy and modify",
        "",
        "&7This is an example placeable block.",
        "&7Right-click to activate."
    };

    // ==================== CONSTRUCTOR ====================
    public ExampleBlock(@Nonnull ItemGroup category) {
        super(category, createItemStack(), RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    // ==================== ITEM STACK CREATION ====================
    private static SlimefunItemStack createItemStack() {
        return new SlimefunItemStack(ID, MATERIAL, DISPLAY_NAME, LORE);
    }

    // ==================== RECIPE ====================
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.STONE), new ItemStack(Material.STONE), new ItemStack(Material.STONE),
            new ItemStack(Material.STONE), SlimefunItems.STEEL_PLATE, new ItemStack(Material.STONE),
            new ItemStack(Material.STONE), new ItemStack(Material.STONE), new ItemStack(Material.STONE)
        };
    }

    // ==================== HANDLERS ====================
    @Override
    public void preRegister() {
        addItemHandler(onPlace());
        addItemHandler(onUse());
    }

    private BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(@Nonnull org.bukkit.event.block.BlockPlaceEvent event) {
                // Called when the block is placed
                // Store owner, spawn entities, etc.
                WWSFPlugin plugin = WWSFPlugin.getInstance();
                BlockStorage.addBlockInfo(
                    event.getBlockPlaced(),
                    plugin.getOwnerKey().getKey(),
                    event.getPlayer().getUniqueId().toString()
                );
                event.getPlayer().sendMessage("&7Example block placed!");
            }
        };
    }

    private ItemUseHandler onUse() {
        return (PlayerRightClickEvent event) -> {
            // Called when player right-clicks the placed block
            event.cancel();
            Block block = event.getClickedBlock().orElse(null);
            if (block == null) return;
            
            // Your custom block logic here
            // Example: spawn particles, give effects, etc.
        };
    }

    // ==================== CONFIG-DRIVEN STATS ====================
    // These read from config.yml under items.WWSF_EXAMPLE_BLOCK.<key>
    
    public double getBlastResistance() {
        return ItemConfigHelper.getDouble(this, "example.blast-resistance", 6.0);
    }

    public int getEffectRadius() {
        return ItemConfigHelper.getInt(this, "example.radius", 5);
    }

    public long getCooldownMs() {
        return ItemConfigHelper.getLong(this, "example.cooldown-ms", 1000L);
    }

    // ==================== REGISTRATION ====================
    public static void registerStatic(WWSFPlugin plugin) {
        new ExampleBlock(WWSFCategory.WWSF).register(plugin);
    }
}