package com.wwsf.multiblock;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TrenchCannonDetectedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location origin;
    private final int rotation;
    private boolean cancelled;

    public TrenchCannonDetectedEvent(@Nonnull Player player, @Nonnull Location origin, int rotation) {
        this.player = player;
        this.origin = origin;
        this.rotation = rotation;
        this.cancelled = false;
    }

    @Nonnull
    public Player getPlayer() { return player; }

    @Nonnull
    public Location getOrigin() { return origin; }

    public int getRotation() { return rotation; }

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
