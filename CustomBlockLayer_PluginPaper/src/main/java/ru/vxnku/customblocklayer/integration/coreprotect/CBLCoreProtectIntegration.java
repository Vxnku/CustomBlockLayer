package ru.vxnku.customblocklayer.integration.coreprotect;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

import java.util.logging.Level;

/**
 * Main manager for CoreProtect integration in CustomBlockLayer.
 * Coordinates logging, history storage, rollback handling, and inspection.
 */
public class CBLCoreProtectIntegration {
    private static CBLCoreProtectIntegration instance;
    private final CustomBlockLayerPlugin plugin;
    private CBLHistoryStorage historyStorage;
    private CoreProtectRollbackHandler rollbackHandler;
    private CoreProtectCommandInterceptor commandInterceptor;
    private CoreProtectInspectListener inspectListener;
    private boolean enabled = false;

    public CBLCoreProtectIntegration(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
    }

    public static void init(@NotNull CustomBlockLayerPlugin plugin) {
        if (instance != null) {
            instance.shutdown();
        }

        instance = new CBLCoreProtectIntegration(plugin);
        instance.start();
    }

    public static void shutdown() {
        if (instance != null) {
            instance.stop();
            instance = null;
        }
    }

    public static CBLCoreProtectIntegration getInstance() {
        return instance;
    }

    private void start() {
        try {
            // 1. Initialize SQLite History Storage
            this.historyStorage = new CBLHistoryStorage(plugin);

            // 2. Initialize Safe CoreProtect API Logger
            CBLCoreProtectLogger.init();

            // 3. Initialize Rollback Handler
            this.rollbackHandler = new CoreProtectRollbackHandler(plugin, historyStorage);

            // 4. Initialize and Register Command Interceptor
            this.commandInterceptor = new CoreProtectCommandInterceptor(plugin, rollbackHandler);
            Bukkit.getPluginManager().registerEvents(commandInterceptor, plugin);

            // 5. Initialize and Register Inspect Listener
            this.inspectListener = new CoreProtectInspectListener(plugin, historyStorage);
            Bukkit.getPluginManager().registerEvents(inspectListener, plugin);

            this.enabled = true;

            if (CBLCoreProtectLogger.isAvailable()) {
                plugin.getLogger().info("[CBL-CoreProtect] Интеграция с CoreProtect успешно активирована (селективный откат b:cbl:*, инспектор, SQLite история)!");
            } else {
                plugin.getLogger().info("[CBL-CoreProtect] CoreProtect не обнаружен. Автономная история CBL и селективный откат продолжают работать в штатном режиме.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[CBL-CoreProtect] Ошибка при запуске интеграции CoreProtect: " + e.getMessage(), e);
        }
    }

    private void stop() {
        if (historyStorage != null) {
            historyStorage.shutdown();
        }
        this.enabled = false;
        plugin.getLogger().info("[CBL-CoreProtect] Интеграция с CoreProtect остановлена.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    public CBLHistoryStorage getHistoryStorage() {
        return historyStorage;
    }

    @Nullable
    public CoreProtectRollbackHandler getRollbackHandler() {
        return rollbackHandler;
    }

    /**
     * Logs a custom block placement.
     */
    public static void logBlockPlace(@Nullable Player player, @NotNull Block block, @NotNull String customId) {
        if (instance == null || !instance.isEnabled()) return;

        String userName = (player != null) ? player.getName() : "#Console";
        long now = System.currentTimeMillis() / 1000L;

        // 1. Record in local CBL history
        if (instance.historyStorage != null && block.getWorld() != null) {
            instance.historyStorage.logAction(
                now, block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(),
                1, customId, userName
            );
        }

        // 2. Log in CoreProtect
        CBLCoreProtectLogger.logPlacement(userName, block.getLocation(), block.getType(), block.getBlockData());
    }

    /**
     * Logs a custom block break.
     */
    public static void logBlockBreak(@Nullable Player player, @NotNull Block block, @NotNull String customId) {
        if (instance == null || !instance.isEnabled()) return;

        String userName = (player != null) ? player.getName() : "#World";
        long now = System.currentTimeMillis() / 1000L;

        // 1. Record in local CBL history
        if (instance.historyStorage != null && block.getWorld() != null) {
            instance.historyStorage.logAction(
                now, block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(),
                0, customId, userName
            );
        }

        // 2. Log in CoreProtect
        CBLCoreProtectLogger.logRemoval(userName, block.getLocation(), block.getType(), block.getBlockData());
    }
}
