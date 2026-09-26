package com.wwsf.items;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.wwsf.multiblock.MarkerManager;
import com.wwsf.multiblock.MultiblockRegistration;
import com.wwsf.util.Messages;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;

public class TriggerItemListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        if (!(sfItem instanceof TriggerBlock triggerBlock)) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        if (!player.hasPermission("wwsf.use")) {
            player.sendMessage(Messages.color("&cYou don't have permission to use this."));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        MarkerManager.MarkerData marker = MultiblockRegistration.registerAt(player, clicked, triggerBlock.getCannonDefinition());
        if (marker == null) {
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getAmount() > 1) {
            mainHand.setAmount(mainHand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(org.bukkit.Material.AIR));
        }
        player.updateInventory();
        player.sendMessage(Messages.color("&aTrigger placed and marker created for &f" + marker.definition().getName() + "&a."));
    }
}
