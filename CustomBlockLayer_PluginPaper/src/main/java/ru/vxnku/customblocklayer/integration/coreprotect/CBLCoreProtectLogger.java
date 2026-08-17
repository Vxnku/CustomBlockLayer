package ru.vxnku.customblocklayer.integration.coreprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;

/**
 * Safe reflection-based wrapper for CoreProtectAPI.
 * Guarantees zero runtime crashes even if CoreProtect is missing or modified.
 */
public class CBLCoreProtectLogger {
    private static Object coreProtectAPI = null;
    private static boolean available = false;
    private static boolean initialized = false;
    private static int apiVersion = 0;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        try {
            Plugin cpPlugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");
            if (cpPlugin == null) {
                Bukkit.getLogger().info("[CBL-CoreProtect] CoreProtect не найден. Логирование блоков в CoreProtect отключено.");
                return;
            }
            if (!cpPlugin.isEnabled()) {
                Bukkit.getLogger().info("[CBL-CoreProtect] CoreProtect найден, но еще не включен.");
                return;
            }

            java.lang.reflect.Method getAPIMethod = cpPlugin.getClass().getMethod("getAPI");
            coreProtectAPI = getAPIMethod.invoke(cpPlugin);

            if (coreProtectAPI == null) {
                Bukkit.getLogger().info("[CBL-CoreProtect] CoreProtect API вернул null.");
                return;
            }

            java.lang.reflect.Method isEnabledMethod = coreProtectAPI.getClass().getMethod("isEnabled");
            boolean apiEnabled = (boolean) isEnabledMethod.invoke(coreProtectAPI);
            if (!apiEnabled) {
                Bukkit.getLogger().info("[CBL-CoreProtect] CoreProtect API отключен в конфигурации CoreProtect.");
                coreProtectAPI = null;
                return;
            }

            java.lang.reflect.Method apiVersionMethod = coreProtectAPI.getClass().getMethod("APIVersion");
            apiVersion = (int) apiVersionMethod.invoke(coreProtectAPI);
            if (apiVersion < 9) {
                Bukkit.getLogger().warning("[CBL-CoreProtect] CoreProtect API версия " + apiVersion + " слишком старая (требуется >= 9).");
                coreProtectAPI = null;
                return;
            }

            available = true;
            Bukkit.getLogger().info("[CBL-CoreProtect] CoreProtect API v" + apiVersion + " успешно подключен!");
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[CBL-CoreProtect] Не удалось подключить CoreProtect API: " + e.getMessage());
            coreProtectAPI = null;
        }
    }

    public static boolean isAvailable() {
        if (!available || coreProtectAPI == null) {
            init();
        }
        return available && coreProtectAPI != null;
    }

    public static int getApiVersion() {
        return apiVersion;
    }

    public static void logRemoval(@NotNull String user, @NotNull Location location, @NotNull Material material, @Nullable BlockData blockData) {
        if (!isAvailable()) {
            Bukkit.getLogger().info("[CBL-CoreProtect-Debug] logRemoval пропущен: CoreProtect API недоступен.");
            return;
        }
        try {
            java.lang.reflect.Method method = coreProtectAPI.getClass().getMethod(
                "logRemoval", String.class, Location.class, Material.class, BlockData.class
            );
            Object result = method.invoke(coreProtectAPI, user, location, material, blockData);
            Bukkit.getLogger().info("[CBL-CoreProtect-Debug] logRemoval: user=" + user + ", pos=" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ", mat=" + material + " -> результат: " + result);
        } catch (Exception e) {
            try {
                java.lang.reflect.Method methodState = coreProtectAPI.getClass().getMethod(
                    "logRemoval", String.class, org.bukkit.block.BlockState.class
                );
                Object result = methodState.invoke(coreProtectAPI, user, location.getBlock().getState());
                Bukkit.getLogger().info("[CBL-CoreProtect-Debug] logRemoval(BlockState): user=" + user + " -> результат: " + result);
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.WARNING, "[CBL-CoreProtect] Ошибка вызова logRemoval: " + e.getMessage(), e);
            }
        }
    }

    public static void logPlacement(@NotNull String user, @NotNull Location location, @NotNull Material material, @Nullable BlockData blockData) {
        if (!isAvailable()) {
            Bukkit.getLogger().info("[CBL-CoreProtect-Debug] logPlacement пропущен: CoreProtect API недоступен.");
            return;
        }
        try {
            java.lang.reflect.Method method = coreProtectAPI.getClass().getMethod(
                "logPlacement", String.class, Location.class, Material.class, BlockData.class
            );
            Object result = method.invoke(coreProtectAPI, user, location, material, blockData);
            Bukkit.getLogger().info("[CBL-CoreProtect-Debug] logPlacement: user=" + user + ", pos=" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ", mat=" + material + " -> результат: " + result);
        } catch (Exception e) {
            try {
                java.lang.reflect.Method methodState = coreProtectAPI.getClass().getMethod(
                    "logPlacement", String.class, org.bukkit.block.BlockState.class
                );
                Object result = methodState.invoke(coreProtectAPI, user, location.getBlock().getState());
                Bukkit.getLogger().info("[CBL-CoreProtect-Debug] logPlacement(BlockState): user=" + user + " -> результат: " + result);
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.WARNING, "[CBL-CoreProtect] Ошибка вызова logPlacement: " + e.getMessage(), e);
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static List<String[]> performLookup(int time, @Nullable List<String> restrictUsers, @Nullable List<String> excludeUsers,
                                             @Nullable List<Object> restrictBlocks, @Nullable List<Object> excludeBlocks,
                                             @Nullable List<Integer> actionList, int radius, @Nullable Location radiusLocation) {
        if (!isAvailable()) return null;
        try {
            java.lang.reflect.Method method = coreProtectAPI.getClass().getMethod(
                "performLookup", int.class, List.class, List.class, List.class, List.class, List.class, int.class, Location.class
            );
            return (List<String[]>) method.invoke(coreProtectAPI, time, restrictUsers, excludeUsers, restrictBlocks, excludeBlocks, actionList, radius, radiusLocation);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[CBL-CoreProtect] Ошибка вызова performLookup: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static List<String[]> blockLookup(@NotNull Block block, int time) {
        if (!isAvailable()) return null;
        try {
            java.lang.reflect.Method method = coreProtectAPI.getClass().getMethod("blockLookup", Block.class, int.class);
            return (List<String[]>) method.invoke(coreProtectAPI, block, time);
        } catch (Exception e) {
            return null;
        }
    }
}
