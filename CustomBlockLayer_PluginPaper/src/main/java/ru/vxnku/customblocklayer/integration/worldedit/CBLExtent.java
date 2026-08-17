package ru.vxnku.customblocklayer.integration.worldedit;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTagType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

import java.util.HashSet;
import java.util.Set;

public class CBLExtent extends AbstractDelegateExtent {
    private final World bukkitWorld;
    private final Region selection;
    private final Set<Long> affectedChunkKeys = new HashSet<>();
    private boolean scheduledBroadcast = false;

    public CBLExtent(@NotNull Extent extent, @NotNull World bukkitWorld, @Nullable Region selection) {
        super(extent);
        this.bukkitWorld = bukkitWorld;
        this.selection = selection;
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        BaseBlock base = super.getFullBlock(position);
        Location loc = new Location(bukkitWorld, position.x(), position.y(), position.z());
        String customId = CustomBlockLayerPlugin.getInstance().getStorageManager().getStorage().getBlock(loc);
        if (customId != null) {
            LinCompoundTag.Builder builder = LinCompoundTag.builder();
            if (base.getNbtReference() != null && base.getNbtReference().getValue() != null) {
                builder = base.getNbtReference().getValue().toBuilder();
            }
            builder.put("id", LinStringTag.of("customblocklayer:cbl"));
            builder.put("cbl", LinStringTag.of(customId));
            return base.toBaseBlock(LazyReference.computed(builder.build()));
        }
        return base;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        Location loc = new Location(bukkitWorld, position.x(), position.y(), position.z());
        int cx = position.x() >> 4;
        int cz = position.z() >> 4;
        long chunkKey = (((long) cx) << 32) | (cz & 0xFFFFFFFFL);

        // 1. Check if set via active Pattern
        String customId = CBLPatternContext.getAndRemovePending(position);

        // 2. Check if set via cloned / stacked BaseBlock NBT tag
        if (customId == null && block instanceof BaseBlock baseBlock) {
            LazyReference<LinCompoundTag> ref = baseBlock.getNbtReference();
            if (ref != null && ref.getValue() != null) {
                LinStringTag tag = ref.getValue().findTag("cbl", LinTagType.stringTag());
                if (tag != null) {
                    customId = tag.value();
                }
            }
        }

        // 3. Fallback for //stack / //move / //copy: Map from source selection region coordinates!
        if (customId == null && selection != null) {
            try {
                BlockVector3 min = selection.getMinimumPoint();
                BlockVector3 max = selection.getMaximumPoint();
                int sx = max.x() - min.x() + 1;
                int sy = max.y() - min.y() + 1;
                int sz = max.z() - min.z() + 1;
                if (sx > 0 && sy > 0 && sz > 0) {
                    int dx = Math.floorMod(position.x() - min.x(), sx);
                    int dy = Math.floorMod(position.y() - min.y(), sy);
                    int dz = Math.floorMod(position.z() - min.z(), sz);
                    Location srcLoc = new Location(bukkitWorld, min.x() + dx, min.y() + dy, min.z() + dz);
                    
                    // Only clone if the target is NOT the exact source position itself
                    if (!srcLoc.equals(loc)) {
                        String srcCustomId = CustomBlockLayerPlugin.getInstance().getStorageManager().getStorage().getBlock(srcLoc);
                        if (srcCustomId != null) {
                            customId = srcCustomId;
                            CustomBlockLayerPlugin.getInstance().getLogger().info("[CBL-WE] Cloned " + srcCustomId + " from " + srcLoc.getBlockX() + "," + srcLoc.getBlockY() + "," + srcLoc.getBlockZ() + " to " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (customId != null) {
            CustomBlockLayerPlugin.getInstance().getStorageManager().getStorage().setBlock(loc, customId);
            CustomBlockLayerPlugin.getInstance().getNetworkManager().sendSetBlock(loc, customId);
            affectedChunkKeys.add(chunkKey);
        } else {
            if (CustomBlockLayerPlugin.getInstance().getStorageManager().getStorage().hasBlock(loc)) {
                CustomBlockLayerPlugin.getInstance().getStorageManager().getStorage().removeBlock(loc);
                CustomBlockLayerPlugin.getInstance().getNetworkManager().sendClearBlock(loc);
                affectedChunkKeys.add(chunkKey);
            }
        }

        if (!scheduledBroadcast) {
            scheduledBroadcast = true;
            Bukkit.getScheduler().runTask(CustomBlockLayerPlugin.getInstance(), this::flushChunkUpdates);
        }

        return super.setBlock(position, block);
    }

    private void flushChunkUpdates() {
        scheduledBroadcast = false;
        for (long key : affectedChunkKeys) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            if (bukkitWorld.isChunkLoaded(cx, cz)) {
                Chunk chunk = bukkitWorld.getChunkAt(cx, cz);
                CustomBlockLayerPlugin.getInstance().getNetworkManager().broadcastChunkBlocks(chunk);
            }
        }
        affectedChunkKeys.clear();
    }

    @Override
    protected Operation commitBefore() {
        flushChunkUpdates();
        return super.commitBefore();
    }
}
