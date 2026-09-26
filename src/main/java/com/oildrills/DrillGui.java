package com.oildrills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import com.wwsf.util.CustomModelDataUtil;

public class DrillGui implements Listener {

    private static final int SLOT_TOGGLE = 13;
    private static final int SLOT_INFO = 4;

    private final DrillManager drillManager;

    public DrillGui(DrillManager drillManager) {
        this.drillManager = drillManager;
    }

    void open(Player player, Location drillLoc, ActiveDrillData data) {
        DrillMenuHolder holder = new DrillMenuHolder(drillLoc);
        Inventory inv = Bukkit.createInventory(holder, 27, "§8Oil Drill Control");
        holder.setInventory(inv);

        fillBorder(inv);
        inv.setItem(SLOT_INFO, buildInfoItem(drillLoc.getBlock(), data));
        inv.setItem(SLOT_TOGGLE, buildToggleItem(data.isEnabled()));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DrillMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() != SLOT_TOGGLE) {
            return;
        }

        ActiveDrillData data = drillManager.getDrill(holder.getDrillLocation());
        if (data == null) {
            player.closeInventory();
            return;
        }

        Block drill = holder.getDrillLocation().getBlock();
        boolean nowOn = !data.isEnabled();
        DrillOperationCheck startCheck = DrillOperationCheck.OK;

        if (nowOn) {
            startCheck = drillManager.evaluateForStart(drill, data);
        }

        data.setEnabled(nowOn);
        drillManager.onDrillPowerChanged(holder.getDrillLocation(), nowOn);
        drillManager.saveDrills();

        event.getInventory().setItem(SLOT_TOGGLE, buildToggleItem(nowOn));
        event.getInventory().setItem(SLOT_INFO, buildInfoItem(drill, data));

        if (nowOn) {
            int coal = drillManager.countCoalInHopper(drill);
            int need = drillManager.coalRequiredPerBarrel(data);
            if (startCheck == DrillOperationCheck.OK) {
                player.sendMessage("§aDrill §lON§a — needs §c" + need + " coal§a per barrel (§e" + coal + "§a available).");
            } else {
                player.sendMessage("§eDrill §lON§e — waiting: §7" + startCheck.message());
                player.sendMessage("§7It will start automatically when that is fixed.");
            }
        } else {
            player.sendMessage("§cDrill §lOFF§c — idle.");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // no-op; holder is per-open inventory
    }

    private ItemStack buildToggleItem(boolean enabled) {
        Material mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(enabled ? "§a§lRUNNING" : "§c§lSTOPPED");
            meta.setLore(List.of(
                "§7Click to toggle power.",
                enabled ? "§8Piston is pumping." : "§8Drill is idle."
            ));
            CustomModelDataUtil.apply(meta, enabled ? 50 : 51);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildInfoItem(Block drill, ActiveDrillData data) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6" + data.tier().getDisplayPrefix());
            boolean chunksLoaded = drillManager.areRigChunksLoaded(drill, data);
            int coalHave = chunksLoaded ? drillManager.countCoalInHopper(drill) : 0;
            int coalNeed = drillManager.coalRequiredPerBarrel(data);
            boolean coalOk = coalHave >= coalNeed;
            DrillOperationCheck check = data.isEnabled()
                ? drillManager.evaluateOperation(drill, data, false)
                : DrillOperationCheck.DISABLED;

            String statusLine = switch (check) {
                case OK -> data.isEnabled() ? "§aON §7— pumping" : "§cOFF";
                case DISABLED -> "§cOFF";
                default -> "§eON §7— §c" + check.message();
            };

            meta.setLore(List.of(
                "§7Status: " + statusLine,
                "§7Next barrel: §e" + formatTime(data.secondsUntilNext()),
                "§7Coal: " + (coalOk ? "§a" : "§c") + coalHave + "§7/§c" + coalNeed + " §7(required)",
                "§7Coal hopper: " + (chunksLoaded && DrillManager.findCoalHopper(drill) != null ? "§a✓" : "§c✗"),
                "§7Output chest: " + (chunksLoaded && DrillManager.findOutputInventory(drill) != null ? "§a✓" : "§c✗"),
                "§7Rig structure: " + (chunksLoaded && DrillStructure.isComplete(drill, data.tier()) ? "§a✓" : "§c✗")
            ));
            CustomModelDataUtil.apply(meta, 52);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatTime(long seconds) {
        if (seconds <= 0) {
            return "§aReady";
        }
        long min = seconds / 60;
        long sec = seconds % 60;
        if (min > 0) {
            return min + "m " + sec + "s";
        }
        return sec + "s";
    }

    private static void fillBorder(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            CustomModelDataUtil.apply(meta, 1);
            glass.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, glass);
            }
        }
    }
}
