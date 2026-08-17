package ru.vxnku.customblocklayer.cache;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.Map;

/**
 * Thread-safe Spatial Hash Map storing active Custom Block Overrides for the current client world.
 */
public class BlockOverrideCache {
    private static final Long2ObjectMap<String> POS_TO_ID = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectMap<LongSet> CHUNK_TO_BLOCKS = new Long2ObjectOpenHashMap<>();

    public static void init() {
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            onChunkUnload(chunk.getPos().x, chunk.getPos().z);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            clear();
        });
    }

    public static synchronized void set(BlockPos pos, String customId) {
        long packedPos = pos.asLong();
        POS_TO_ID.put(packedPos, customId);

        long packedChunk = ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
        CHUNK_TO_BLOCKS.computeIfAbsent(packedChunk, k -> new LongOpenHashSet()).add(packedPos);

        scheduleRerender(pos);
    }

    public static synchronized void remove(BlockPos pos) {
        long packedPos = pos.asLong();
        POS_TO_ID.remove(packedPos);

        long packedChunk = ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
        LongSet blocks = CHUNK_TO_BLOCKS.get(packedChunk);
        if (blocks != null) {
            blocks.remove(packedPos);
            if (blocks.isEmpty()) {
                CHUNK_TO_BLOCKS.remove(packedChunk);
            }
        }

        scheduleRerender(pos);
    }

    public static synchronized void setChunkBlocks(int chunkX, int chunkZ, Map<Integer, String> blocks) {
        long packedChunk = ChunkPos.toLong(chunkX, chunkZ);
        LongSet oldBlocks = CHUNK_TO_BLOCKS.remove(packedChunk);
        if (oldBlocks != null) {
            for (long packedPos : oldBlocks) {
                POS_TO_ID.remove(packedPos);
            }
        }

        if (blocks != null && !blocks.isEmpty()) {
            LongSet newBlocks = new LongOpenHashSet();
            int originX = chunkX << 4;
            int originZ = chunkZ << 4;

            for (Map.Entry<Integer, String> entry : blocks.entrySet()) {
                int packedLocal = entry.getKey();
                String customId = entry.getValue();
                
                int localX = packedLocal & 0xF;
                int localZ = (packedLocal >> 4) & 0xF;
                int localY = (short)(packedLocal >> 8); // Supports full -64..320 range

                BlockPos worldPos = new BlockPos(originX + localX, localY, originZ + localZ);
                long packedPos = worldPos.asLong();

                POS_TO_ID.put(packedPos, customId);
                newBlocks.add(packedPos);
            }
            CHUNK_TO_BLOCKS.put(packedChunk, newBlocks);
        }

        scheduleChunkRerender(chunkX, chunkZ);
    }

    public static synchronized void onChunkUnload(int chunkX, int chunkZ) {
        long packedChunk = ChunkPos.toLong(chunkX, chunkZ);
        LongSet blocks = CHUNK_TO_BLOCKS.remove(packedChunk);
        if (blocks != null) {
            for (long packedPos : blocks) {
                POS_TO_ID.remove(packedPos);
            }
        }
    }

    public static synchronized void clear() {
        POS_TO_ID.clear();
        CHUNK_TO_BLOCKS.clear();
    }

    public static String get(BlockPos pos) {
        return POS_TO_ID.get(pos.asLong());
    }

    public static boolean has(BlockPos pos) {
        return POS_TO_ID.containsKey(pos.asLong());
    }

    public static int size() {
        return POS_TO_ID.size();
    }

    private static void scheduleRerender(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.worldRenderer != null && client.world != null) {
            client.execute(() -> {
                client.worldRenderer.scheduleBlockRenders(
                    pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
                );
            });
        }
    }

    private static void scheduleChunkRerender(int chunkX, int chunkZ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.worldRenderer != null && client.world != null) {
            client.execute(() -> {
                int minX = chunkX << 4;
                int minZ = chunkZ << 4;
                int maxX = minX + 15;
                int maxZ = minZ + 15;
                client.worldRenderer.scheduleBlockRenders(
                    minX, client.world != null ? client.world.getBottomY() : -64, minZ,
                    maxX, client.world != null ? client.world.getTopY() : 320, maxZ
                );
            });
        }
    }
}
