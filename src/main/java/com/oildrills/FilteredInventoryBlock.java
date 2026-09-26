package com.oildrills;

import java.util.function.Consumer;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * Inventory block preset that can reserve cargo-node input slots for specific ingredients.
 *
 * <p>The stock {@link InventoryBlock} preset exposes every input slot for every inserted item.
 * Multi-ingredient machines can therefore be filled completely by one ingredient and become
 * clogged. Implementations provide the slots that are valid for the item cargo is moving.</p>
 */
interface FilteredInventoryBlock extends InventoryBlock {

    int[] getInputSlotsFor(ItemStack item);

    @Override
    default void createPreset(
            SlimefunItem item,
            String title,
            Consumer<BlockMenuPreset> setup
    ) {
        new BlockMenuPreset(item.getId(), title) {
            @Override
            public void init() {
                setup.accept(this);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.INSERT
                        ? FilteredInventoryBlock.this.getInputSlots()
                        : FilteredInventoryBlock.this.getOutputSlots();
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(
                    DirtyChestMenu menu,
                    ItemTransportFlow flow,
                    ItemStack transportedItem
            ) {
                return flow == ItemTransportFlow.INSERT
                        ? FilteredInventoryBlock.this.getInputSlotsFor(transportedItem)
                        : FilteredInventoryBlock.this.getOutputSlots();
            }

            @Override
            public boolean canOpen(Block block, Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                        || item.canUse(player, false)
                        && Slimefun.getProtectionManager().hasPermission(
                                player,
                                block.getLocation(),
                                Interaction.INTERACT_BLOCK
                        );
            }
        };
    }
}
