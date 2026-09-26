package com.wwsf.items;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

import com.wwsf.WWSFPlugin;
import com.wwsf.multiblock.CannonDefinition;

/**
 * Fallback entry point for cannons whose trigger has reverted to a vanilla tripwire hook
 * (Slimefun block state lost). Right-clicking the hook validates the multiblock directly and
 * starts aiming, so the weapon keeps working regardless of the trigger block's state.
 */
public class TriggerInteractListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.TRIPWIRE_HOOK) {
            return;
        }

        // If it is still a working Slimefun TriggerBlock, let its own handler deal with it.
        SlimefunItem sf = BlockStorage.check(block.getLocation());
        if (sf instanceof TriggerBlock) {
            return;
        }

        CannonDefinition matched = TriggerBlock.findMatching(block);
        if (matched == null) {
            // Try to auto-detect and register the multiblock at this hook.
            com.wwsf.multiblock.MarkerManager.MarkerData auto = com.wwsf.multiblock.MultiblockRegistration.registerAt(event.getPlayer(), block);
            if (auto == null) {
                return;
            }
            com.wwsf.items.MultiblockInteractListener.startAimFromMarker(event.getPlayer(), auto, event);
            return;
        }

        event.setCancelled(true);
        com.wwsf.artillery.ArtillerySpec weaponSpec = matched.getWeaponSpec();
        int[] rotated = matched.rotateOffset(matched.triggerOffsetX(), matched.triggerOffsetZ(), matched.getLastRotation());
        Location origin = block.getLocation().subtract(rotated[0], matched.triggerOffsetY(), rotated[1]);
        Location barrelTip = matched.getBarrelTip(origin, matched.getLastRotation());
        Vector barrelDir = matched.getBarrelDirection(matched.getLastRotation());
        java.util.UUID vesselId = com.wwsf.vehicle.VehicleQuery.vesselIdAt(block.getLocation()).orElse(null);
        WWSFPlugin.getInstance().getAimingManager().startAiming(event.getPlayer(), origin, weaponSpec, barrelTip, barrelDir, vesselId);
    }
}
