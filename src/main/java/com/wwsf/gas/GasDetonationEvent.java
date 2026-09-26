package com.wwsf.gas;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Fired when a Gas Bomb detonates and releases a gas cloud.
 * Allows other systems to detect gas deployments without tight coupling.
 */
public class GasDetonationEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Location detonationLocation;
    private final ItemStack sourceItem;
    private final Player thrower;

    public GasDetonationEvent(@Nonnull Location detonationLocation, @Nullable ItemStack sourceItem, @Nullable Player thrower) {
        this.detonationLocation = detonationLocation.clone();
        this.sourceItem = sourceItem != null ? sourceItem.clone() : null;
        this.thrower = thrower;
    }

    @Nonnull
    public Location getDetonationLocation() {
        return detonationLocation;
    }

    @Nullable
    public ItemStack getSourceItem() {
        return sourceItem != null ? sourceItem.clone() : null;
    }

    @Nullable
    public Player getThrower() {
        return thrower;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}