package com.oildrills;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;

public class DrillBreakHandler extends BlockBreakHandler {
    
    public DrillBreakHandler() {
        super(true, true);
    }
    
    @Override
    public void onPlayerBreak(BlockBreakEvent event, ItemStack item, List<ItemStack> drops) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        
        if (sfItem instanceof OilDrillItem drillItem) {
            DrillManager manager = OilDrillsPlugin.getInstance().getDrillManager();
            if (manager != null) {
                // Use the handleDrillBreak method
                manager.handleDrillBreak(player, block.getLocation());
            }
        }
    }
}
