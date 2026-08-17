package ru.vxnku.customblocklayer.integration.coreprotect;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.registry.CustomBlockEntry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles selective rollbacks and restores for CustomBlockLayer blocks.
 */
public class CoreProtectRollbackHandler {
    private final CustomBlockLayerPlugin plugin;
    private final CBLHistoryStorage historyStorage;

    public CoreProtectRollbackHandler(@NotNull CustomBlockLayerPlugin plugin, @NotNull CBLHistoryStorage historyStorage) {
        this.plugin = plugin;
        this.historyStorage = historyStorage;
    }

    /**
     * Executes a selective rollback for custom blocks.
     */
    public void executeSelectiveRollback(@NotNull CommandSender sender, @NotNull String customIdFilter,
                                         int timeSeconds, int radius, @NotNull Location centerLoc,
                                         @Nullable String userFilter, boolean isRestore) {
        World world = centerLoc.getWorld();
        if (world == null) {
            sender.sendMessage(Component.text("§c[CBL] Не удалось определить мир для отката."));
            return;
        }

        sender.sendMessage(Component.text("§8[§6CBL§8] §7Поиск кастомных блоков для отката (§ecbl/" + customIdFilter + "§7)..."));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int cx = centerLoc.getBlockX();
            int cy = centerLoc.getBlockY();
            int cz = centerLoc.getBlockZ();

            int minX = (radius > 0) ? cx - radius : -30000000;
            int maxX = (radius > 0) ? cx + radius : 30000000;
            int minY = (radius > 0) ? Math.max(world.getMinHeight(), cy - radius) : world.getMinHeight();
            int maxY = (radius > 0) ? Math.min(world.getMaxHeight() - 1, cy + radius) : world.getMaxHeight() - 1;
            int minZ = (radius > 0) ? cz - radius : -30000000;
            int maxZ = (radius > 0) ? cz + radius : 30000000;

            long minTimestamp = (System.currentTimeMillis() / 1000L) - timeSeconds;

            List<CBLHistoryStorage.HistoryEntry> entries = historyStorage.findEntriesForRollback(
                world.getName(), minX, maxX, minY, maxY, minZ, maxZ, minTimestamp, userFilter, customIdFilter
            );

            if (entries.isEmpty()) {
                sender.sendMessage(Component.text("§8[§6CBL§8] §eЗаписей действий для cbl:" + customIdFilter + " не найдено за указанный период."));
                return;
            }

            // Execute world modifications synchronously on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                int modifiedCount = 0;
                Set<String> processedCoords = new HashSet<>();

                for (CBLHistoryStorage.HistoryEntry entry : entries) {
                    String coordKey = entry.x + "," + entry.y + "," + entry.z;
                    if (processedCoords.contains(coordKey)) {
                        continue;
                    }
                    processedCoords.add(coordKey);

                    Location blockLoc = new Location(world, entry.x, entry.y, entry.z);
                    if (radius > 0 && blockLoc.distanceSquared(centerLoc) > (radius * radius)) {
                        continue;
                    }

                    Block targetBlock = world.getBlockAt(entry.x, entry.y, entry.z);

                    if (!isRestore) {
                        // Standard Rollback:
                        // 1. If the logged action was BREAK (0), rollback means RESTORING the block
                        if (entry.action == 0) {
                            if (targetBlock.getType().isAir() || targetBlock.isPassable()) {
                                Material baseMat = resolveBaseMaterial(entry.customId);
                                targetBlock.setType(baseMat, false);

                                if (entry.customId.contains("vertical_slab") && targetBlock.getBlockData() instanceof TrapDoor trapDoor) {
                                    trapDoor.setOpen(true);
                                    targetBlock.setBlockData(trapDoor, false);
                                }

                                plugin.getStorageManager().getStorage().setBlock(blockLoc, entry.customId);
                                plugin.getNetworkManager().sendSetBlock(blockLoc, entry.customId);
                                CBLCoreProtectLogger.logPlacement("#Rollback", blockLoc, baseMat, targetBlock.getBlockData());
                                modifiedCount++;
                            }
                        }
                        // 2. If the logged action was PLACE (1), rollback means REMOVING the placed block
                        else if (entry.action == 1) {
                            String currentCbl = plugin.getStorageManager().getStorage().getBlock(blockLoc);
                            if (currentCbl != null && currentCbl.equalsIgnoreCase(entry.customId)) {
                                plugin.getStorageManager().getStorage().removeBlock(blockLoc);
                                targetBlock.setType(Material.AIR, false);
                                plugin.getNetworkManager().sendClearBlock(blockLoc);
                                CBLCoreProtectLogger.logRemoval("#Rollback", blockLoc, targetBlock.getType(), targetBlock.getBlockData());
                                modifiedCount++;
                            }
                        }
                    } else {
                        // Restore mode:
                        // Re-apply placements that were undone
                        if (entry.action == 1) {
                            Material baseMat = resolveBaseMaterial(entry.customId);
                            targetBlock.setType(baseMat, false);

                            if (entry.customId.contains("vertical_slab") && targetBlock.getBlockData() instanceof TrapDoor trapDoor) {
                                trapDoor.setOpen(true);
                                targetBlock.setBlockData(trapDoor, false);
                            }

                            plugin.getStorageManager().getStorage().setBlock(blockLoc, entry.customId);
                            plugin.getNetworkManager().sendSetBlock(blockLoc, entry.customId);
                            modifiedCount++;
                        }
                    }
                }

                sender.sendMessage(
                    Component.text("§8[§6CBL§8] §aУспешно обработано §e" + modifiedCount + " §aблоков cbl/" + customIdFilter +
                                   " §7(затронуто записей: " + entries.size() + ")")
                );
            });
        });
    }

    private Material resolveBaseMaterial(String customId) {
        CustomBlockEntry entry = plugin.getBlockRegistry().getBlock(customId);
        if (entry != null && entry.getBaseMaterial() != null) {
            return entry.getBaseMaterial();
        }
        String idLower = customId.toLowerCase();
        if (idLower.contains("stair")) return Material.QUARTZ_STAIRS;
        if (idLower.contains("slab")) return Material.QUARTZ_SLAB;
        if (idLower.contains("crate") || idLower.contains("plank")) return Material.OAK_PLANKS;
        if (idLower.contains("trapdoor") || idLower.contains("vertical_slab")) return Material.OAK_TRAPDOOR;
        return Material.STONE;
    }
}
