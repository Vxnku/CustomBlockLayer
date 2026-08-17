package ru.vxnku.customblocklayer.integration.coreprotect;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.registry.CustomBlockEntry;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enriches CoreProtect inspection output (/co i) with CustomBlockLayer metadata.
 * Only displays when inspection mode (/co i) is active for the player.
 */
public class CoreProtectInspectListener implements Listener {
    private final CustomBlockLayerPlugin plugin;
    private final CBLHistoryStorage historyStorage;
    private final Set<UUID> inspectingPlayers = ConcurrentHashMap.newKeySet();

    public CoreProtectInspectListener(@NotNull CustomBlockLayerPlugin plugin, @NotNull CBLHistoryStorage historyStorage) {
        this.plugin = plugin;
        this.historyStorage = historyStorage;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().trim().toLowerCase();
        if (msg.equals("/co i") || msg.equals("/co inspect") || msg.equals("/coreprotect inspect") || msg.equals("/coreprotect i")) {
            Player player = event.getPlayer();
            if (inspectingPlayers.contains(player.getUniqueId())) {
                inspectingPlayers.remove(player.getUniqueId());
            } else {
                inspectingPlayers.add(player.getUniqueId());
            }
        } else if (msg.startsWith("/co i ") || msg.startsWith("/co inspect ") || msg.startsWith("/coreprotect i ") || msg.startsWith("/coreprotect inspect ")) {
            Player player = event.getPlayer();
            if (msg.contains("on") || msg.contains("1") || msg.contains("true")) {
                inspectingPlayers.add(player.getUniqueId());
            } else if (msg.contains("off") || msg.contains("0") || msg.contains("false")) {
                inspectingPlayers.remove(player.getUniqueId());
            } else {
                if (inspectingPlayers.contains(player.getUniqueId())) {
                    inspectingPlayers.remove(player.getUniqueId());
                } else {
                    inspectingPlayers.add(player.getUniqueId());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        inspectingPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Prevent double trigger from main hand and off hand
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        // Strictly only trigger if player has /co i turned on
        if (!inspectingPlayers.contains(player.getUniqueId())) {
            return;
        }

        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Location loc = clickedBlock.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "";
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        // Query active block & history asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String currentCustomId = plugin.getStorageManager().getStorage().getBlock(loc);
            List<CBLHistoryStorage.HistoryEntry> history = historyStorage.getHistoryAt(worldName, x, y, z, 3);

            if (currentCustomId == null && history.isEmpty()) {
                return;
            }

            // Schedule display slightly after CoreProtect's message (2 ticks)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || !inspectingPlayers.contains(player.getUniqueId())) return;

                player.sendMessage(Component.text("----- CustomBlockLayer (" + x + "/" + y + "/" + z + ") -----", NamedTextColor.GOLD));

                if (currentCustomId != null) {
                    String displayName = resolveDisplayName(currentCustomId);
                    player.sendMessage(
                        Component.text("  Текущий блок: ", NamedTextColor.GRAY)
                            .append(Component.text("cbl/" + currentCustomId, NamedTextColor.YELLOW))
                            .append(Component.text(" (" + displayName + ")", NamedTextColor.WHITE))
                    );
                }

                if (!history.isEmpty()) {
                    long now = System.currentTimeMillis() / 1000L;
                    for (CBLHistoryStorage.HistoryEntry entry : history) {
                        long diff = Math.max(0, now - entry.timestamp);
                        String timeFormatted = formatTimeAgo(diff);
                        String actionText = (entry.action == 1) ? "установил" : "сломал";
                        NamedTextColor actionColor = (entry.action == 1) ? NamedTextColor.GREEN : NamedTextColor.RED;

                        player.sendMessage(
                            Component.text("  " + timeFormatted + " назад - ", NamedTextColor.GRAY)
                                .append(Component.text(entry.user + " ", NamedTextColor.AQUA))
                                .append(Component.text(actionText + " ", actionColor))
                                .append(Component.text("cbl/" + entry.customId, NamedTextColor.GOLD))
                        );
                    }
                }
            }, 2L);
        });
    }

    private String resolveDisplayName(String customId) {
        CustomBlockEntry entry = plugin.getBlockRegistry().getBlock(customId);
        if (entry != null && entry.getDisplayName() != null && !entry.getDisplayName().isEmpty()) {
            return entry.getDisplayName();
        }
        return customId;
    }

    private String formatTimeAgo(long seconds) {
        if (seconds < 60) return seconds + "с";
        if (seconds < 3600) return (seconds / 60) + "м";
        if (seconds < 86400) return (seconds / 3600) + "ч";
        return (seconds / 86400) + "д";
    }
}
