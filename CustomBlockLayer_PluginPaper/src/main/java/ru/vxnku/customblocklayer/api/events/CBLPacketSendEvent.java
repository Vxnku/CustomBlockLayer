package ru.vxnku.customblocklayer.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a CustomBlockLayer plugin message packet is dispatched to a player.
 */
public class CBLPacketSendEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final String channel;
    private byte[] data;

    public CBLPacketSendEvent(@NotNull Player player, @NotNull String channel, @NotNull byte[] data) {
        this.player = player;
        this.channel = channel;
        this.data = data;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getChannel() {
        return channel;
    }

    @NotNull
    public byte[] getData() {
        return data;
    }

    public void setData(@NotNull byte[] data) {
        this.data = data;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
