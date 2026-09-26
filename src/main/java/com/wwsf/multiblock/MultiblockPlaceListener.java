package com.wwsf.multiblock;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.wwsf.WWSFPlugin;
import com.wwsf.artillery.ArtilleryDefinition;
import com.wwsf.artillery.ArtilleryRegistry;
import com.wwsf.artillery.ArtillerySpec;
import com.wwsf.items.AbstractArtilleryBlock;
import com.wwsf.util.Messages;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

public class MultiblockPlaceListener implements Listener {

    private final WWSFPlugin plugin;

    public MultiblockPlaceListener(WWSFPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        var block = event.getBlock();
        
        AbstractArtilleryBlock artillery = AbstractArtilleryBlock.getArtilleryAt(block);
        if (artillery != null) {
            if (com.wwsf.vehicle.VehicleQuery.isAvailable()
                    && com.wwsf.vehicle.VehicleQuery.vesselIdAt(block.getLocation()).isPresent()) {
                return;
            }
            event.setCancelled(true);
            plugin.getAimingManager().cancelAimAt(block.getLocation());
            
            MarkerManager.MarkerData marker = MarkerManager.findMarkerForBlock(block.getLocation());
            if (marker == null) {
                marker = MarkerManager.findMarkerAtLocation(block.getLocation());
            }
            if (marker == null) {
                marker = MarkerManager.findMarkerAtEntity(block.getLocation());
            }
            if (marker != null) {
                String weaponId = marker.definition().getWeaponSpec().id();
                WWSFPlugin.getInstance().getLogger().info("[MultiblockPlaceListener] Breaking marker for weapon: " + weaponId);
                ItemStack triggerItem = ArtilleryRegistry.getTriggerItem(weaponId);
                if (triggerItem != null) {
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), triggerItem);
                    WWSFPlugin.getInstance().getLogger().info("[MultiblockPlaceListener] Dropped trigger item: " + triggerItem.getType());
                } else {
                    WWSFPlugin.getInstance().getLogger().warning("[MultiblockPlaceListener] No trigger item found for weapon: " + weaponId);
                }
                MarkerManager.removeMarker(marker.markerId());
            }
            
            event.getPlayer().sendMessage(Messages.get("messages.multiblock-broken", "&7Weapon dismantled."));
            return;
        }

        for (var offset : MultiblockPattern.getAdjacentOffsets()) {
            var relative = block.getRelative(offset.x(), offset.y(), offset.z());
            artillery = AbstractArtilleryBlock.getArtilleryAt(relative);
            if (artillery != null && artillery.getSpec().multiblock() != null) {
                if (artillery.getSpec().multiblock().isValid(relative)) {
                    if (com.wwsf.vehicle.VehicleQuery.isAvailable()
                            && com.wwsf.vehicle.VehicleQuery.vesselIdAt(relative.getLocation()).isPresent()) {
                        return;
                    }
                    event.setCancelled(true);
                    artillery.getSpec().multiblock().breakMultiblock(relative);
                    plugin.getAimingManager().cancelAimAt(relative.getLocation());
                    event.getPlayer().sendMessage(Messages.get("messages.multiblock-broken", "&7Weapon dismantled."));
                    return;
                }
            }
        }

        MarkerManager.MarkerData marker = MarkerManager.findMarkerForBlock(block.getLocation());
        if (marker == null) {
            marker = MarkerManager.findMarkerAtLocation(block.getLocation());
        }
        if (marker == null) {
            marker = MarkerManager.findMarkerAtEntity(block.getLocation());
        }
        if (marker != null) {
            String weaponId = marker.definition().getWeaponSpec().id();
            WWSFPlugin.getInstance().getLogger().info("[MultiblockPlaceListener] Breaking marker for weapon: " + weaponId);
            ItemStack triggerItem = ArtilleryRegistry.getTriggerItem(weaponId);
            if (triggerItem != null) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), triggerItem);
                WWSFPlugin.getInstance().getLogger().info("[MultiblockPlaceListener] Dropped trigger item: " + triggerItem.getType());
            } else {
                WWSFPlugin.getInstance().getLogger().warning("[MultiblockPlaceListener] No trigger item found for weapon: " + weaponId);
            }
            MarkerManager.removeMarker(marker.markerId());
        }
    }
}