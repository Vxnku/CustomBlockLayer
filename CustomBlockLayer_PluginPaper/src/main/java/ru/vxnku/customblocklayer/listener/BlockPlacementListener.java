package ru.vxnku.customblocklayer.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.api.events.CBLBlockPlaceEvent;
import ru.vxnku.customblocklayer.item.CBLItemFactory;

public class BlockPlacementListener implements Listener {
    private final CustomBlockLayerPlugin plugin;

    public BlockPlacementListener(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String customId = CBLItemFactory.getCustomBlockId(item);

        if (customId != null) {
            Player player = event.getPlayer();
            CBLBlockPlaceEvent cblEvent = new CBLBlockPlaceEvent(event.getBlock(), player, customId);
            Bukkit.getPluginManager().callEvent(cblEvent);

            if (cblEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            Location loc = event.getBlock().getLocation();
            plugin.getLogger().info("[CBL] Игрок " + player.getName() + " установил кастомный блок '" + customId + "' на " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            
            // For vertical slabs: force open=true and align facing only if NOT sneaking
            if (customId.contains("vertical_slab") && event.getBlock().getBlockData() instanceof TrapDoor trapDoor) {
                trapDoor.setOpen(true);

                if (!player.isSneaking()) {
                    Location belowLoc = loc.clone().subtract(0, 1, 0);
                    String belowId = plugin.getStorageManager().getStorage().getBlock(belowLoc);
                    if (belowId != null && belowId.contains("vertical_slab")) {
                        if (loc.getWorld().getBlockAt(belowLoc).getBlockData() instanceof TrapDoor belowTrapDoor) {
                            trapDoor.setFacing(belowTrapDoor.getFacing());
                        }
                    }
                }

                event.getBlock().setBlockData(trapDoor, false);
            }

            plugin.getStorageManager().getStorage().setBlock(loc, customId);
            plugin.getNetworkManager().sendSetBlock(loc, customId);

            // Log to CoreProtect and CBL history
            ru.vxnku.customblocklayer.integration.coreprotect.CBLCoreProtectIntegration.logBlockPlace(player, event.getBlock(), customId);

            // Handle double-block plants / tall blocks (EXCLUDE trapdoors!)
            if (event.getBlock().getBlockData() instanceof Bisected bisected && !(event.getBlock().getBlockData() instanceof TrapDoor)) {
                if (bisected.getHalf() == Bisected.Half.BOTTOM) {
                    Location topLoc = loc.clone().add(0, 1, 0);
                    plugin.getStorageManager().getStorage().setBlock(topLoc, customId);
                    plugin.getNetworkManager().sendSetBlock(topLoc, customId);
                    ru.vxnku.customblocklayer.integration.coreprotect.CBLCoreProtectIntegration.logBlockPlace(player, topLoc.getBlock(), customId);
                }
            }
        }
    }
}
