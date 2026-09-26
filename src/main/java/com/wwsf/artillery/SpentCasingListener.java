package com.wwsf.artillery;

import javax.annotation.Nonnull;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Keeps spent-casing carrier states stable without taking ownership of normal
 * note blocks or the unpowered states reserved for sandbags.
 */
public final class SpentCasingListener implements Listener {

    private final JavaPlugin plugin;

    public SpentCasingListener(@Nonnull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (SpentCasingBlock.isCasing(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
            || event.getAction() != Action.RIGHT_CLICK_BLOCK
            || event.getClickedBlock() == null
            || !SpentCasingBlock.isCasing(event.getClickedBlock())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNotePlay(NotePlayEvent event) {
        int note = SpentCasingBlock.casingNote(event.getBlock());
        if (note < 0) {
            return;
        }
        event.setCancelled(true);
        restoreNextTick(event.getBlock(), note);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        int note = SpentCasingBlock.casingNote(event.getBlock());
        if (note < 0) {
            return;
        }
        event.setNewCurrent(event.getOldCurrent());
        restoreNextTick(event.getBlock(), note);
    }

    private void restoreNextTick(Block block, int note) {
        org.bukkit.Location location = block.getLocation();
        plugin.getServer().getScheduler().runTask(
            plugin,
            () -> SpentCasingBlock.restore(location.getBlock(), note)
        );
    }
}
