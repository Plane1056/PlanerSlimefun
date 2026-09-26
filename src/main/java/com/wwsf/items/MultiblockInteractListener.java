package com.wwsf.items;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.wwsf.WWSFPlugin;
import com.wwsf.artillery.ArtillerySpec;
import com.wwsf.artillery.AimingManager;
import com.wwsf.multiblock.CannonDefinition;
import com.wwsf.multiblock.MarkerManager;
import com.wwsf.util.Messages;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

public class MultiblockInteractListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null) {
            SlimefunItem sfItem = SlimefunItem.getByItem(item);
            if (sfItem instanceof TriggerBlock) {
                return;
            }
        }

        MarkerManager.MarkerData marker = MarkerManager.findMarkerForBlock(clicked.getLocation());
        if (marker == null) {
            return;
        }
        if (marker == null) {
            return;
        }

        startAimFromMarker(player, marker, event);
    }

    /**
     * Shared aiming entry point used both by the right-click-on-structure handler and the
     * vanilla tripwire-hook fallback. Validates the marker, enforces single-user locking,
     * and starts aiming in marker mode.
     */
    public static void startAimFromMarker(@Nonnull Player player, @Nonnull MarkerManager.MarkerData marker, @Nullable org.bukkit.event.player.PlayerInteractEvent event) {
        if (!MarkerManager.isCoreValid(marker)) {
            player.sendMessage(Messages.color("&cThis weapon's structure is no longer valid."));
            MarkerManager.removeMarker(marker.markerId());
            if (event != null) {
                event.setCancelled(true);
            }
            return;
        }

        WWSFPlugin plugin = WWSFPlugin.getInstance();
        java.util.UUID currentUser = plugin.getAimingManager().getWeaponUser(marker.core());
        if (currentUser != null && !currentUser.equals(player.getUniqueId())) {
            Player other = plugin.getServer().getPlayer(currentUser);
            String name = other != null ? other.getName() : "Someone";
            player.sendMessage(Messages.color("&cThis weapon is currently in use by &f" + name + "&c."));
            if (event != null) {
                event.setCancelled(true);
            }
            return;
        }

        if (event != null) {
            event.setCancelled(true);
        }

        if (player.isSneaking()) {
            plugin.getAimingManager().stopAiming(player);
            player.sendMessage(Messages.color("&7Aiming cancelled."));
            return;
        }

        CannonDefinition def = marker.definition();
        ArtillerySpec weaponSpec = def.getWeaponSpec();
        Location origin = marker.core();
        Location barrelEnd = def.getBarrelTip(origin, marker.rotation());
        Vector barrelDir = def.getBarrelDirection(marker.rotation());

        java.util.UUID vesselId = com.wwsf.vehicle.VehicleQuery.vesselIdAt(origin).orElse(null);
        plugin.getAimingManager().startAiming(player, origin, weaponSpec, barrelEnd, barrelDir, vesselId, marker.markerId());
    }
}
