package com.wwsf.tools;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.WWSFItem;
import com.wwsf.setup.ResourcesCategory;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;

public class EntrenchingTool extends WWSFItem implements Listener {

    public static final String ID = "WWSF_ENTRENCHING_TOOL";
    private static final int CUSTOM_MODEL_DATA = 10012;
    
    private static final int DURABILITY = 700;
    private static final double SPEED_MULTIPLIER = 2.0;
    
    private static final Set<Material> SOFT_BLOCKS = EnumSet.of(
        Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.PODZOL,
        Material.ROOTED_DIRT,
        Material.SAND, Material.RED_SAND, Material.GRAVEL,
        Material.CLAY, Material.MUD, Material.MUDDY_MANGROVE_ROOTS
    );

    public EntrenchingTool(@Nonnull ItemGroup category) {
        super(category, createStack(), RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    @Override
    public void postRegister() {
        super.postRegister();
        setRecipeOutput(getItem());
    }

    @Override
    public @Nonnull ItemStack getItem() {
        ItemStack item = super.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Nonnull
    private static SlimefunItemStack createStack() {
        SlimefunItemStack stack = new SlimefunItemStack(
            ID,
            Material.IRON_SHOVEL,
            ChatColor.GOLD + "Entrenching Tool",
            meta -> {
                com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
                meta.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "WW1 trench-digging tool",
                    "",
                    ChatColor.YELLOW + "2x speed on soft terrain",
                    ChatColor.YELLOW + "2x3 area break",
                    ChatColor.GRAY + "Digs trenches quickly"
                ));
            }
        );
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            meta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "WW1 trench-digging tool",
                "",
                ChatColor.YELLOW + "2x speed on soft terrain",
                ChatColor.YELLOW + "2x3 area break",
                ChatColor.GRAY + "Digs trenches quickly"
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        
        if (hand == null || hand.getType() != Material.IRON_SHOVEL) {
            return;
        }
        
        SlimefunItem sfItem = SlimefunItem.getByItem(hand);
        if (sfItem == null || !(sfItem instanceof EntrenchingTool)) {
            return;
        }

        Block targetBlock = event.getBlock();
        if (!isSoftTerrain(targetBlock.getType())) {
            return; // Not soft terrain, let vanilla handle it
        }

        event.setCancelled(true);
        
        Vector direction = player.getFacing().getDirection();
        int areaWidth = 2;
        int areaDepth = 3;
        
        int blocksBroken = 0;
        
        for (int depth = 0; depth < areaDepth; depth++) {
            for (int width = 0; width < areaWidth; width++) {
                Block blockToBreak = getBlockInArea(targetBlock, direction, depth, width);
                
                if (blockToBreak == null || blockToBreak.getType() == Material.AIR) {
                    continue;
                }
                
                if (!isSoftTerrain(blockToBreak.getType())) {
                    continue; // Skip hard blocks
                }
                
                // Break the block
                blockToBreak.breakNaturally();
                blocksBroken++;
            }
        }
        
        // Consume durability
        if (blocksBroken > 0) {
            consumeDurability(hand, blocksBroken, player);
        }
    }

    private Block getBlockInArea(Block center, Vector facing, int depthOffset, int widthOffset) {
        Vector right = new Vector(-facing.getZ(), 0, facing.getX()).normalize();
        
        Vector offset = facing.clone().multiply(depthOffset);
        offset.add(right.clone().multiply(widthOffset));
        
        return center.getRelative(offset.getBlockX(), 0, offset.getBlockZ());
    }

    private boolean isSoftTerrain(Material material) {
        return SOFT_BLOCKS.contains(material);
    }

    private void consumeDurability(ItemStack tool, int amount, Player player) {
        if (tool == null || tool.getType() == Material.AIR) {
            return;
        }
        
        // Use vanilla durability system
        short durability = tool.getDurability();
        short maxDurability = tool.getType().getMaxDurability();
        short newDurability = (short) (durability + amount);
        
        if (newDurability >= maxDurability) {
            // Tool breaks
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(ChatColor.RED + "Your entrenching tool has broken!");
        } else {
            tool.setDurability(newDurability);
        }
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT),
            new ItemStack(Material.OAK_PLANKS), new ItemStack(Material.IRON_SHOVEL), new ItemStack(Material.OAK_PLANKS),
            new ItemStack(Material.STRING), new ItemStack(Material.STICK), new ItemStack(Material.STRING)
        };
    }

    public static void registerStatic(WWSFPlugin plugin) {
        new EntrenchingTool(ResourcesCategory.RESOURCES).register(plugin);
        plugin.getServer().getPluginManager().registerEvents(new EntrenchingTool(ResourcesCategory.RESOURCES), plugin);
    }
}
