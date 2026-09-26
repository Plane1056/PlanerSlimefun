package com.wwsf.multiblock;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FieldCannonDetectedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location anchorLocation;
    private final int rotation;
    private final Location origin;
    private boolean cancelled;

    public FieldCannonDetectedEvent(
        @Nonnull Player player,
        @Nonnull Location anchorLocation,
        int rotation,
        @Nonnull Location origin
    ) {
        this.player = player;
        this.anchorLocation = anchorLocation;
        this.rotation = rotation;
        this.origin = origin;
        this.cancelled = false;
    }

    @Nonnull
    public Player getPlayer() { return player; }

    @Nonnull
    public Location getAnchorLocation() { return anchorLocation; }

    public int getRotation() { return rotation; }

    @Nonnull
    public Location getOrigin() { return origin; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    @Nonnull
    public HandlerList getHandlers() { return HANDLERS; }

    @Nonnull
    public static HandlerList getHandlerList() { return HANDLERS; }
}
