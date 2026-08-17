package ru.vxnku.customblocklayer.listener;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

public class PlayerChunkTrackingListener implements Listener {
    private final CustomBlockLayerPlugin plugin;

    public PlayerChunkTrackingListener(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(PlayerChunkLoadEvent event) {
        plugin.getNetworkManager().sendChunkBlocks(event.getPlayer(), event.getChunk());
    }
}
