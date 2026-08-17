package ru.vxnku.customblocklayer.storage;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Storage abstraction for Custom Block metadata.
 */
public interface ICBLStorage {
    void setBlock(@NotNull Location loc, @NotNull String customId);
    void removeBlock(@NotNull Location loc);
    @Nullable String getBlock(@NotNull Location loc);
    boolean hasBlock(@NotNull Location loc);
    @NotNull Map<Integer, String> getChunkBlocks(@NotNull Chunk chunk);
    void saveChunk(@NotNull Chunk chunk);
    void loadChunk(@NotNull Chunk chunk);
    void shutdown();
}
