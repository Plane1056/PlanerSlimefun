package com.oildrills;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

/**
 * Reliable Bukkit-level interaction handler for the single-use Dowsing Rod.
 */
public final class DowsingRodListener implements Listener {

    static final double DETECTION_RANGE_BLOCKS = 50.0;

    private final ChunkOilStorage storage;

    public DowsingRodListener(ChunkOilStorage storage) {
        this.storage = storage;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGroundClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isDowsingRod(item)) {
            return;
        }

        // Bukkit may also emit an off-hand interaction for the same click.
        if (event.getHand() == EquipmentSlot.OFF_HAND
                && isDowsingRod(event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }

        event.setCancelled(true);
        consumeOne(event);

        ChunkOilStorage.OilFieldTarget target = storage.findNearbyField(
                event.getPlayer().getLocation(),
                DETECTION_RANGE_BLOCKS
        );
        if (target == null) {
            event.getPlayer().playSound(
                    event.getPlayer().getLocation(),
                    Sound.BLOCK_FIRE_EXTINGUISH,
                    0.8F,
                    1.2F
            );
            event.getPlayer().sendMessage(
                    "§cNo oil field was detected within ±50 blocks. The Dowsing Rod snaps."
            );
            return;
        }

        storage.markSurveyedField(event.getPlayer().getWorld(), target.fieldId());
        storage.save();

        int percent = target.capacity() == 0
                ? 0
                : (int) Math.round(target.remaining() * 100.0 / target.capacity());
        event.getPlayer().playSound(
                event.getPlayer().getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                1.0F,
                0.8F
        );
        event.getPlayer().sendMessage("§8§m------------------------------");
        event.getPlayer().sendMessage("§6§lOil Field Detected §7(within ±50 blocks)");
        event.getPlayer().sendMessage("§7Field: §e" + target.name());
        event.getPlayer().sendMessage("§7Grade: " + target.rarity().getLabel());
        event.getPlayer().sendMessage(
                "§7Shared reserves: §e" + target.remaining() + "§7/§e" + target.capacity()
                        + " §8(" + percent + "%)"
        );
        event.getPlayer().sendMessage("§aThis field is now available for drilling.");
        event.getPlayer().sendMessage("§8The Dowsing Rod snaps after use.");
        event.getPlayer().sendMessage("§8§m------------------------------");
    }

    private static boolean isDowsingRod(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
        if (slimefunItem != null && DowsingRodItem.ID.equals(slimefunItem.getId())) {
            return true;
        }

        if (OilItems.DOWSING_ROD_ITEM != null && OilItems.DOWSING_ROD_ITEM.isItem(item)) {
            return true;
        }

        // Compatibility fallback for an older rod, or one whose Slimefun tag was
        // stripped by another inventory plugin.
        if (item.getType() != Material.BLAZE_ROD) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        String name = ChatColor.stripColor(meta.getDisplayName());
        if (!"Dowsing Rod".equalsIgnoreCase(name)) {
            return false;
        }

        return meta.hasLore() && meta.getLore().stream()
                .map(ChatColor::stripColor)
                .filter(line -> line != null)
                .anyMatch(line -> line.toLowerCase(Locale.ROOT).contains("oil field"));
    }

    private static void consumeOne(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            event.getPlayer().getInventory().setItemInOffHand(null);
        } else {
            event.getPlayer().getInventory().setItemInMainHand(null);
        }
    }
}
