package com.wwsf.integration;

import com.wwsf.WWSFPlugin;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Removes potion-container remnants after the nine-bottle gas-drum recipe. */
public final class GasDrumCraftCleanupListener implements Listener {

    private static final String GAS_DRUM_ID = "WWSF_GAS_RELEASE_DRUM";
    private final WWSFPlugin plugin;

    public GasDrumCraftCleanupListener(WWSFPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEnhancedCraft(PlayerInteractEvent event) {
        Block table = event.getClickedBlock();
        if (table == null || table.getType() != Material.CRAFTING_TABLE
            || !(table.getRelative(0, -1, 0).getState() instanceof Dispenser dispenser)) {
            return;
        }

        // Slimefun resolves the recipe later in the same interaction. Inspect the
        // backing dispenser on the following tick, after its output has appeared.
        plugin.getServer().getScheduler().runTask(plugin,
            () -> removeBottleRemainders(dispenser.getInventory()));
    }

    static int removeBottleRemainders(Inventory inventory) {
        if (!containsGasDrum(inventory)) {
            return 0;
        }
        int removed = 0;
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length && removed < 9; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType() != Material.GLASS_BOTTLE
                || SlimefunItem.getByItem(stack) != null) {
                continue;
            }
            int take = Math.min(stack.getAmount(), 9 - removed);
            int remaining = stack.getAmount() - take;
            inventory.setItem(slot, remaining == 0 ? null : stack.asQuantity(remaining));
            removed += take;
        }
        return removed;
    }

    private static boolean containsGasDrum(Inventory inventory) {
        for (ItemStack stack : inventory.getContents()) {
            SlimefunItem item = SlimefunItem.getByItem(stack);
            if (item != null && GAS_DRUM_ID.equals(item.getId())) {
                return true;
            }
        }
        return false;
    }
}
