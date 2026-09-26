package com.wwsf.gas;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wwsf.WWSFPlugin;

/**
 * /wwsf gas immunity — opens a GUI for opped players to register items
 * that grant immunity to chlorine gas.
 *
 * <p>Layout (9-slot inventory):
 * <pre>
 *   │ I  I  I  I  S  B  B  B  B │
 *   I = item input slots (0-3) — players place helmets/armor here
 *   S = SAVE button (slot 4, green pane)
 *   B = border (slots 5-8, black glass)
 * </pre>
 */
public class GasImmunityCommand implements Listener {

    private static final int INVENTORY_SIZE = 9;
    private static final int[] INPUT_SLOTS = {0, 1, 2, 3};
    private static final int SAVE_SLOT = 4;
    private static final int[] BORDER_SLOTS = {5, 6, 7, 8};

    private final WWSFPlugin plugin;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    private final ItemStack saveFiller;
    private final ItemStack borderFiller;

    public GasImmunityCommand(@Nonnull WWSFPlugin plugin) {
        this.plugin = plugin;
        this.saveFiller = createPane(Material.GREEN_STAINED_GLASS_PANE, "§a§lSAVE IMMUNITY ITEMS");
        this.borderFiller = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
    }

    public void open(@Nonnull Player player) {
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, "§8Gas Immunity Items");

        for (int slot : BORDER_SLOTS) {
            inv.setItem(slot, borderFiller);
        }
        inv.setItem(SAVE_SLOT, saveFiller);

        // Input slots 0-3 are left empty

        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = openInventories.get(player.getUniqueId());
        if (inv == null || !event.getInventory().equals(inv)) return;

        int slot = event.getRawSlot();

        // Block border
        if (isBorderSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        // Handle save button
        if (slot == SAVE_SLOT) {
            event.setCancelled(true);
            saveItems(player, inv);
            return;
        }

        // Input slots — allow free interaction
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = openInventories.get(player.getUniqueId());
        if (inv == null || !event.getInventory().equals(inv)) return;

        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < INVENTORY_SIZE && isBorderSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory inv = openInventories.remove(player.getUniqueId());
        if (inv == null) return;

        // Return items in input slots to player
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR && !isFiller(item)) {
                player.getInventory().addItem(item).values()
                    .forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
            }
        }
    }

    private void saveItems(@Nonnull Player player, @Nonnull Inventory inv) {
        int saved = 0;
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR || isFiller(item)) continue;

            String base64 = GasImmunityConfig.serializeItem(item);
            GasImmunityConfig.addItem(base64);
            saved++;
        }

        if (saved == 0) {
            player.sendMessage("§cNo items found in the input slots! Place items in slots 1-4 first.");
            return;
        }

        GasImmunityConfig.save();
        player.sendMessage("§aSaved " + saved + " gas immunity item(s)!");
        player.sendMessage("§7These items will now grant immunity to chlorine gas when worn.");

        // Clear input slots
        for (int slot : INPUT_SLOTS) {
            inv.setItem(slot, null);
        }

        player.closeInventory();
    }

    // ========== Utilities ==========

    private static ItemStack createPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private boolean isBorderSlot(int slot) {
        for (int bs : BORDER_SLOTS) {
            if (bs == slot) return true;
        }
        return false;
    }

    private boolean isFiller(ItemStack item) {
        return item.equals(saveFiller) || item.equals(borderFiller);
    }

    /**
     * Cleans up all open GUIs (call on plugin disable).
     */
    public void cleanup() {
        for (Map.Entry<UUID, Inventory> entry : openInventories.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.getOpenInventory().getTopInventory().equals(entry.getValue())) {
                player.closeInventory();
            }
        }
        openInventories.clear();
    }
}