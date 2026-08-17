package ru.vxnku.customblocklayer.integration.worldedit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.registry.CustomBlockEntry;

public class CBLPattern extends AbstractPattern {
    private final CustomBlockEntry entry;
    private final BlockState baseState;

    public CBLPattern(@NotNull CustomBlockEntry entry, @NotNull BlockState baseState) {
        this.entry = entry;
        this.baseState = baseState;
    }

    @NotNull
    public CustomBlockEntry getEntry() {
        return entry;
    }

    @NotNull
    public BlockState getBaseState() {
        return baseState;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        // Tag position in thread-local context so CBLExtent knows to save the customId
        CBLPatternContext.setPending(position, entry.getId());
        return baseState.toBaseBlock();
    }
}
