package ru.vxnku.customblocklayer.api.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a CustomBlockLayer block is about to be placed in the world.
 * Can be cancelled by protection plugins (WorldGuard, Towny, Lands, etc.).
 */
public class CBLBlockPlaceEvent extends BlockEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final String customId;

    public CBLBlockPlaceEvent(@NotNull Block block, @Nullable Player player, @NotNull String customId) {
        super(block);
        this.player = player;
        this.customId = customId;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getCustomId() {
        return customId;
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
