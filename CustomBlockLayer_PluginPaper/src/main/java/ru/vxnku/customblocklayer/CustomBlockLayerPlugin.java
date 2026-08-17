package ru.vxnku.customblocklayer;

import org.bukkit.plugin.java.JavaPlugin;
import ru.vxnku.customblocklayer.command.CBLPluginCommand;
import ru.vxnku.customblocklayer.integration.worldedit.CBLWorldEditIntegration;
import ru.vxnku.customblocklayer.listener.BlockBreakListener;
import ru.vxnku.customblocklayer.listener.BlockPlacementListener;
import ru.vxnku.customblocklayer.listener.PistonMoveListener;
import ru.vxnku.customblocklayer.listener.PlayerChunkTrackingListener;
import ru.vxnku.customblocklayer.listener.PlayerConnectionListener;
import ru.vxnku.customblocklayer.network.NetworkManager;
import ru.vxnku.customblocklayer.registry.ServerBlockRegistry;
import ru.vxnku.customblocklayer.storage.StorageManager;

public final class CustomBlockLayerPlugin extends JavaPlugin {
    private static CustomBlockLayerPlugin instance;
    private StorageManager storageManager;
    private NetworkManager networkManager;
    private ServerBlockRegistry blockRegistry;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 1. Storage
        this.storageManager = new StorageManager(this);

        // 2. Network Channels & Handlers
        this.networkManager = new NetworkManager(this);

        // 3. Server Block Registry (WorldEdit / API model list)
        this.blockRegistry = new ServerBlockRegistry(this);

        // 4. WorldEdit Integration (//set cbl:<id>, //replace, brushes, etc.)
        CBLWorldEditIntegration.init(this);

        // 5. CoreProtect Integration (softdepend, rollback b:cbl:*, history, inspector)
        ru.vxnku.customblocklayer.integration.coreprotect.CBLCoreProtectIntegration.init(this);

        // 6. Register Event Listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChunkTrackingListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlacementListener(this), this);
        getServer().getPluginManager().registerEvents(new ru.vxnku.customblocklayer.listener.BlockInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new PistonMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new ru.vxnku.customblocklayer.listener.VerticalSlabPhysicsListener(this), this);

        // 7. Register Commands
        CBLPluginCommand cmd = new CBLPluginCommand(this);
        if (getCommand("cbl") != null) {
            getCommand("cbl").setExecutor(cmd);
            getCommand("cbl").setTabCompleter(cmd);
        }

        getLogger().info("CustomBlockLayer v" + getPluginMeta().getVersion() + " успешно запущен с поддержкой Paper 1.21.1!");
    }

    @Override
    public void onDisable() {
        ru.vxnku.customblocklayer.integration.coreprotect.CBLCoreProtectIntegration.shutdown();
        if (storageManager != null) {
            storageManager.shutdown();
        }
        getLogger().info("CustomBlockLayer отключен!");
    }

    public static CustomBlockLayerPlugin getInstance() {
        return instance;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public ServerBlockRegistry getBlockRegistry() {
        return blockRegistry;
    }
}
