package ru.vxnku.customblocklayer.storage;

import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

public class StorageManager {
    private final ICBLStorage storage;

    public StorageManager(@NotNull CustomBlockLayerPlugin plugin) {
        String type = plugin.getConfig().getString("storage.type", "pdc").toLowerCase();
        if ("sqlite".equals(type)) {
            plugin.getLogger().info("Используется хранилище SQLite.");
            this.storage = new SQLiteStorage(plugin);
        } else {
            plugin.getLogger().info("Используется нативное хранилище Chunk PDC (PersistentDataContainer).");
            this.storage = new PDCChunkStorage(plugin);
        }
    }

    @NotNull
    public ICBLStorage getStorage() {
        return storage;
    }

    public void shutdown() {
        storage.shutdown();
    }
}
