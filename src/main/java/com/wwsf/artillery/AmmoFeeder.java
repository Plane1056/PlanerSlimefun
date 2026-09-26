package com.wwsf.artillery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.wwsf.WWSFPlugin;
import com.wwsf.ammo.AmmoItem;
import com.wwsf.ammo.AmmoRegistry;
import com.wwsf.ammo.MachineGunAmmoItem;
import com.wwsf.artillery.ShellType;
import com.wwsf.config.ItemConfigHelper;
import com.wwsf.items.AbstractArtilleryBlock;
import com.wwsf.util.Messages;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * Pulls compatible shells from an adjacent chest first, then the player's inventory.
 */
public final class AmmoFeeder {

    private static final BlockFace[] SEARCH_FACES = {
        BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP
    };

    private AmmoFeeder() {
    }

    public static boolean consume(@Nonnull WWSFPlugin plugin, @Nonnull Player player, @Nonnull Location cannon, @Nonnull ShellType shellType) {
        // Creative mode: free ammo
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return true;
        }

        AbstractArtilleryBlock artillery = AbstractArtilleryBlock.getArtilleryAt(cannon.getBlock());
        boolean chestFirst = artillery != null
                ? ItemConfigHelper.getBoolean(artillery, "ammo.chest-first", true)
                : true;
        if (chestFirst && consumeFromAdjacentChests(cannon, shellType)) {
            return true;
        }

        if (consumeFromInventory(player, shellType)) {
            return true;
        }

        if (!chestFirst && consumeFromAdjacentChests(cannon, shellType)) {
            return true;
        }

        player.sendMessage(Messages.get("messages.no-ammo",
            "&cNo compatible ammunition. Load a chest beside the weapon or carry shells."));
        return false;
    }

    public static boolean hasAmmo(@Nonnull Player player, @Nonnull Location cannon, @Nonnull ShellType shellType) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return true;
        }
        if (hasInAdjacentChests(cannon, shellType)) {
            return true;
        }
        return hasInInventory(player, shellType);
    }

    public static @Nullable ShellType resolveAvailableType(@Nonnull Player player, @Nonnull Location cannon) {
        return resolveAvailableType(player, cannon, java.util.EnumSet.allOf(ShellType.class));
    }

    public static @Nullable ShellType resolveAvailableType(
        @Nonnull Player player,
        @Nonnull Location cannon,
        @Nonnull Set<ShellType> allowed
    ) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return allowed.stream().findFirst().orElse(null);
        }

        Block origin = cannon.getBlock();
        for (BlockFace face : SEARCH_FACES) {
            Block adjacent = origin.getRelative(face);
            if (adjacent.getState() instanceof Container container) {
                for (ItemStack stack : container.getInventory().getContents()) {
                    SlimefunItem sfItem = SlimefunItem.getByItem(stack);
                    if (sfItem instanceof MachineGunAmmoItem && allowed.contains(ShellType.STANDARD)) {
                        return ShellType.STANDARD;
                    }
                    if (sfItem instanceof AmmoItem ammo && allowed.contains(ammo.getShellType())) {
                        return ammo.getShellType();
                    }
                }
            }
        }

        for (ItemStack stack : player.getInventory().getContents()) {
            SlimefunItem sfItem = SlimefunItem.getByItem(stack);
            if (sfItem instanceof MachineGunAmmoItem && allowed.contains(ShellType.STANDARD)) {
                return ShellType.STANDARD;
            }
            if (sfItem instanceof AmmoItem ammo && allowed.contains(ammo.getShellType())) {
                return ammo.getShellType();
            }
        }

        return null;
    }

    public static boolean hasCustomAmmo(@Nonnull Player player, @Nonnull Location cannon, @Nonnull String itemId) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return true;
        }
        if (hasInAdjacentChests(cannon, itemId)) {
            return true;
        }
        return hasInInventory(player, itemId);
    }

    public static boolean consumeCustomAmmo(@Nonnull Player player, @Nonnull Location cannon, @Nonnull String itemId) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return true;
        }
        if (consumeFromAdjacentChests(cannon, itemId)) {
            return true;
        }
        if (consumeFromInventory(player, itemId)) {
            return true;
        }
        player.sendMessage(Messages.get("messages.no-ammo",
            "&cNo compatible ammunition. Load a chest beside the weapon or carry shells."));
        return false;
    }

    private static boolean hasInAdjacentChests(@Nonnull Location cannon, @Nonnull String itemId) {
        Block origin = cannon.getBlock();
        for (BlockFace face : SEARCH_FACES) {
            Block adjacent = origin.getRelative(face);
            if (!(adjacent.getState() instanceof Container container)) {
                continue;
            }
            if (containsCustomAmmo(container.getInventory(), itemId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeFromAdjacentChests(@Nonnull Location cannon, @Nonnull String itemId) {
        Block origin = cannon.getBlock();
        for (BlockFace face : SEARCH_FACES) {
            Block adjacent = origin.getRelative(face);
            if (!(adjacent.getState() instanceof Container container)) {
                continue;
            }
            if (takeOneCustom(container.getInventory(), itemId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInInventory(@Nonnull Player player, @Nonnull String itemId) {
        return containsCustomAmmo(player.getInventory(), itemId);
    }

    private static boolean consumeFromInventory(@Nonnull Player player, @Nonnull String itemId) {
        return takeOneCustom(player.getInventory(), itemId);
    }

    private static boolean containsCustomAmmo(@Nullable Inventory inventory, @Nonnull String itemId) {
        if (inventory == null) {
            return false;
        }
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            SlimefunItem sfItem = SlimefunItem.getByItem(stack);
            if (sfItem != null && itemId.equals(sfItem.getId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean takeOneCustom(@Nullable Inventory inventory, @Nonnull String itemId) {
        if (inventory == null) {
            return false;
        }
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            SlimefunItem sfItem = SlimefunItem.getByItem(stack);
            if (sfItem == null || !itemId.equals(sfItem.getId())) {
                continue;
            }
            int amount = stack.getAmount();
            if (amount <= 1) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(amount - 1);
                inventory.setItem(slot, stack);
            }
            return true;
        }
        return false;
    }

    private static boolean consumeFromAdjacentChests(@Nonnull Location cannon, @Nonnull ShellType shellType) {
        Block origin = cannon.getBlock();
        for (BlockFace face : SEARCH_FACES) {
            Block adjacent = origin.getRelative(face);
            if (!(adjacent.getState() instanceof Container container)) {
                continue;
            }
            if (takeOne(container.getInventory(), shellType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInAdjacentChests(@Nonnull Location cannon, @Nonnull ShellType shellType) {
        Block origin = cannon.getBlock();
        for (BlockFace face : SEARCH_FACES) {
            Block adjacent = origin.getRelative(face);
            if (!(adjacent.getState() instanceof Container container)) {
                continue;
            }
            if (containsMatching(container.getInventory(), shellType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeFromInventory(@Nonnull Player player, @Nonnull ShellType shellType) {
        return takeOne(player.getInventory(), shellType);
    }

    private static boolean hasInInventory(@Nonnull Player player, @Nonnull ShellType shellType) {
        return containsMatching(player.getInventory(), shellType);
    }

    private static boolean containsMatching(@Nullable Inventory inventory, @Nonnull ShellType shellType) {
        if (inventory == null) {
            return false;
        }
        for (ItemStack stack : inventory.getContents()) {
            if (AmmoRegistry.hasMatchingAmmo(stack, shellType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean takeOne(@Nullable Inventory inventory, @Nonnull ShellType shellType) {
        if (inventory == null) {
            return false;
        }
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (!AmmoRegistry.hasMatchingAmmo(stack, shellType)) {
                continue;
            }
            int amount = stack.getAmount();
            if (amount <= 1) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(amount - 1);
                inventory.setItem(slot, stack);
            }
            return true;
        }
        return false;
    }
}
