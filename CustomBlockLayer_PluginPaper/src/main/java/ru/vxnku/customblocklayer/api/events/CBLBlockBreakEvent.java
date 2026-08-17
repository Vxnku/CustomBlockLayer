package ru.vxnku.customblocklayer.api.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fired when a CustomBlockLayer block is broken in the world.
 * Custom drops can be modified or cancelled.
 */
public class CBLBlockBreakEvent extends BlockEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final String customId;
    private final List<ItemStack> drops = new ArrayList<>();
    private boolean dropItems = true;

    public CBLBlockBreakEvent(@NotNull Block block, @Nullable Player player, @NotNull String customId, @Nullable ItemStack defaultDrop) {
        super(block);
        this.player = player;
        this.customId = customId;
        if (defaultDrop != null) {
            this.drops.add(defaultDrop);
        }
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getCustomId() {
        return customId;
    }

    @NotNull
    public List<ItemStack> getDrops() {
        return drops;
    }

    public boolean isDropItems() {
        return dropItems;
    }

    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
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
