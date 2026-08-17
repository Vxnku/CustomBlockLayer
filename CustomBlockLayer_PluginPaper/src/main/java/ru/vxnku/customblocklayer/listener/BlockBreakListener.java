package ru.vxnku.customblocklayer.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.api.events.CBLBlockBreakEvent;
import ru.vxnku.customblocklayer.item.CBLItemFactory;

public class BlockBreakListener implements Listener {
    private final CustomBlockLayerPlugin plugin;

    public BlockBreakListener(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (plugin.getStorageManager().getStorage().hasBlock(loc)) {
            String customId = plugin.getStorageManager().getStorage().getBlock(loc);
            if (customId != null) {
                Player player = event.getPlayer();
                
                // Use the exact broken block material
                ItemStack defaultDrop = (player.getGameMode() != GameMode.CREATIVE) 
                    ? CBLItemFactory.createItem(customId, event.getBlock().getType()) 
                    : null;

                CBLBlockBreakEvent cblEvent = new CBLBlockBreakEvent(event.getBlock(), player, customId, defaultDrop);
                Bukkit.getPluginManager().callEvent(cblEvent);

                if (cblEvent.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }

                // Remove current block from storage and notify clients
                plugin.getStorageManager().getStorage().removeBlock(loc);
                plugin.getNetworkManager().sendClearBlock(loc);

                // Log to CoreProtect and CBL history
                ru.vxnku.customblocklayer.integration.coreprotect.CBLCoreProtectIntegration.logBlockBreak(player, event.getBlock(), customId);

                // Handle double-block plants / tall blocks (EXCLUDE trapdoors!)
                if (event.getBlock().getBlockData() instanceof Bisected bisected && !(event.getBlock().getBlockData() instanceof TrapDoor)) {
                    Location otherLoc = (bisected.getHalf() == Bisected.Half.BOTTOM)
                        ? loc.clone().add(0, 1, 0)
                        : loc.clone().subtract(0, 1, 0);

                    if (plugin.getStorageManager().getStorage().hasBlock(otherLoc)) {
                        plugin.getStorageManager().getStorage().removeBlock(otherLoc);
                        plugin.getNetworkManager().sendClearBlock(otherLoc);
                        ru.vxnku.customblocklayer.integration.coreprotect.CBLCoreProtectIntegration.logBlockBreak(player, otherLoc.getBlock(), customId);
                    }
                }

                // Handle custom drops
                if (player.getGameMode() != GameMode.CREATIVE && cblEvent.isDropItems()) {
                    event.setDropItems(false);
                    for (ItemStack drop : cblEvent.getDrops()) {
                        loc.getWorld().dropItemNaturally(loc, drop);
                    }
                }
            }
        }
    }
}
