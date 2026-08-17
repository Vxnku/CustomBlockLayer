package ru.vxnku.customblocklayer.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native Chunk PersistentDataContainer storage.
 * Stores map of packed local coordinate -> customId inside each Chunk's NBT.
 */
public class PDCChunkStorage implements ICBLStorage {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private final NamespacedKey pdcKey;

    // Cache of loaded chunk data: ChunkKey -> Map<Integer, String>
    private final Map<Long, Map<Integer, String>> chunkCache = new ConcurrentHashMap<>();

    public PDCChunkStorage(@NotNull CustomBlockLayerPlugin plugin) {
        this.pdcKey = new NamespacedKey(plugin, "cbl_blocks");
    }

    public static int packLocalPos(int x, int y, int z) {
        return (x & 0xF) | ((z & 0xF) << 4) | ((y & 0xFFFF) << 8);
    }

    public static int unpackLocalX(int packed) { return packed & 0xF; }
    public static int unpackLocalZ(int packed) { return (packed >> 4) & 0xF; }
    public static int unpackLocalY(int packed) { return (short)(packed >> 8); }

    @Override
    public void setBlock(@NotNull Location loc, @NotNull String customId) {
        Chunk chunk = loc.getChunk();
        long chunkKey = chunk.getChunkKey();
        Map<Integer, String> map = chunkCache.computeIfAbsent(chunkKey, k -> loadFromPDC(chunk));
        
        int packed = packLocalPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        map.put(packed, customId);
        saveToPDC(chunk, map);
    }

    @Override
    public void removeBlock(@NotNull Location loc) {
        Chunk chunk = loc.getChunk();
        long chunkKey = chunk.getChunkKey();
        Map<Integer, String> map = chunkCache.computeIfAbsent(chunkKey, k -> loadFromPDC(chunk));

        int packed = packLocalPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (map.remove(packed) != null) {
            saveToPDC(chunk, map);
        }
    }

    @Nullable
    @Override
    public String getBlock(@NotNull Location loc) {
        Chunk chunk = loc.getChunk();
        long chunkKey = chunk.getChunkKey();
        Map<Integer, String> map = chunkCache.computeIfAbsent(chunkKey, k -> loadFromPDC(chunk));
        int packed = packLocalPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return map.get(packed);
    }

    @Override
    public boolean hasBlock(@NotNull Location loc) {
        return getBlock(loc) != null;
    }

    @NotNull
    @Override
    public Map<Integer, String> getChunkBlocks(@NotNull Chunk chunk) {
        long chunkKey = chunk.getChunkKey();
        Map<Integer, String> map = chunkCache.computeIfAbsent(chunkKey, k -> loadFromPDC(chunk));
        return Collections.unmodifiableMap(map);
    }

    @Override
    public void saveChunk(@NotNull Chunk chunk) {
        long chunkKey = chunk.getChunkKey();
        Map<Integer, String> map = chunkCache.get(chunkKey);
        if (map != null) {
            saveToPDC(chunk, map);
        }
    }

    @Override
    public void loadChunk(@NotNull Chunk chunk) {
        long chunkKey = chunk.getChunkKey();
        chunkCache.put(chunkKey, loadFromPDC(chunk));
    }

    @Override
    public void shutdown() {
        chunkCache.clear();
    }

    private Map<Integer, String> loadFromPDC(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (pdc.has(pdcKey, PersistentDataType.STRING)) {
            String json = pdc.get(pdcKey, PersistentDataType.STRING);
            if (json != null && !json.isEmpty()) {
                try {
                    Map<String, String> stringMap = GSON.fromJson(json, MAP_TYPE);
                    if (stringMap != null) {
                        Map<Integer, String> intMap = new ConcurrentHashMap<>();
                        for (Map.Entry<String, String> e : stringMap.entrySet()) {
                            intMap.put(Integer.parseInt(e.getKey()), e.getValue());
                        }
                        return intMap;
                    }
                } catch (Exception ignored) {}
            }
        }
        return new ConcurrentHashMap<>();
    }

    private void saveToPDC(Chunk chunk, Map<Integer, String> map) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (map.isEmpty()) {
            pdc.remove(pdcKey);
        } else {
            Map<String, String> stringMap = new HashMap<>();
            for (Map.Entry<Integer, String> e : map.entrySet()) {
                stringMap.put(String.valueOf(e.getKey()), e.getValue());
            }
            pdc.set(pdcKey, PersistentDataType.STRING, GSON.toJson(stringMap));
        }
    }
}
