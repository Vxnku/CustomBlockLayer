package ru.vxnku.customblocklayer.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

public class PlayerConnectionListener implements Listener {
    private final CustomBlockLayerPlugin plugin;

    public PlayerConnectionListener(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getNetworkManager().getSessionManager().unregisterPlayer(event.getPlayer());
    }
}
