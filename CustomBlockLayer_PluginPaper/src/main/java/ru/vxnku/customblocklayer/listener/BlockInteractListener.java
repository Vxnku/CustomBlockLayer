package ru.vxnku.customblocklayer.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.item.CBLItemFactory;

public class BlockInteractListener implements Listener {
    private final CustomBlockLayerPlugin plugin;

    public BlockInteractListener(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        Location clickedLoc = clickedBlock.getLocation();
        String clickedCustomId = plugin.getStorageManager().getStorage().getBlock(clickedLoc);
        boolean isClickedVerticalSlab = (clickedCustomId != null && clickedCustomId.contains("vertical_slab"));

        ItemStack item = event.getItem();
        String itemCustomId = (item != null) ? CBLItemFactory.getCustomBlockId(item) : null;
        boolean isItemVerticalSlab = (itemCustomId != null && itemCustomId.contains("vertical_slab"));

        // If placing a vertical slab on any block, or placing any block against a vertical slab
        if (isClickedVerticalSlab || isItemVerticalSlab) {
            // Always prevent opening the trapdoor on right click
            event.setUseInteractedBlock(Event.Result.DENY);

            // If player is holding a vertical slab, handle placement directly on server
            if (isItemVerticalSlab && item != null && event.getHand() == EquipmentSlot.HAND) {
                Player player = event.getPlayer();
                BlockFace face = event.getBlockFace();
                Block targetBlock = clickedBlock.getRelative(face);

                if (targetBlock.getType().isAir() || targetBlock.isLiquid() || targetBlock.isPassable()) {
                    BlockFace slabFacing;
                    boolean isSneaking = player.isSneaking();
                    
                    // Inherit facing only if player is NOT sneaking (No Shift)
                    if (!isSneaking && isClickedVerticalSlab && clickedBlock.getBlockData() instanceof TrapDoor clickedTrapDoor) {
                        slabFacing = clickedTrapDoor.getFacing();
                    } else {
                        slabFacing = player.getFacing().getOppositeFace();
                    }

                    // Check if player bounding box intersects with the 8-pixel vertical slab shape
                    BoundingBox playerBox = player.getBoundingBox();
                    BoundingBox slabBox = getSlabBoundingBox(targetBlock.getLocation(), slabFacing);

                    if (!playerBox.overlaps(slabBox)) {
                        // Place vertical slab block
                        targetBlock.setType(item.getType(), false);
                        if (targetBlock.getBlockData() instanceof TrapDoor trapDoor) {
                            trapDoor.setOpen(true);
                            trapDoor.setFacing(slabFacing);
                            targetBlock.setBlockData(trapDoor, false);
                        }

                        Location targetLoc = targetBlock.getLocation();
                        plugin.getStorageManager().getStorage().setBlock(targetLoc, itemCustomId);
                        plugin.getNetworkManager().sendSetBlock(targetLoc, itemCustomId);

                        // Log to CoreProtect and CBL history
                        ru.vxnku.customblocklayer.integration.coreprotect.CBLCoreProtectIntegration.logBlockPlace(player, targetBlock, itemCustomId);

                        // Player arm animation
                        player.swingMainHand();

                        // Consume item in survival
                        if (player.getGameMode() != GameMode.CREATIVE) {
                            item.setAmount(item.getAmount() - 1);
                        }

                        event.setCancelled(true);
                        return;
                    }
                }
            }

            // For other solid blocks placed against a vertical slab
            if (item != null && item.getType().isBlock()) {
                event.setUseItemInHand(Event.Result.ALLOW);
            } else if (item == null || item.getType().isAir()) {
                event.setCancelled(true);
            }
        }
    }

    private BoundingBox getSlabBoundingBox(Location loc, BlockFace facing) {
        double minX = loc.getBlockX();
        double maxX = loc.getBlockX() + 1.0;
        double minY = loc.getBlockY();
        double maxY = loc.getBlockY() + 1.0;
        double minZ = loc.getBlockZ();
        double maxZ = loc.getBlockZ() + 1.0;

        switch (facing) {
            case NORTH -> minZ += 0.5; // South half
            case SOUTH -> maxZ -= 0.5; // North half
            case WEST  -> minX += 0.5; // East half
            case EAST  -> maxX -= 0.5; // West half
            default    -> minZ += 0.5;
        }

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        String customId = plugin.getStorageManager().getStorage().getBlock(event.getBlock().getLocation());
        if (customId != null && customId.contains("vertical_slab")) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        String customId = plugin.getStorageManager().getStorage().getBlock(event.getBlock().getLocation());
        if (customId != null && customId.contains("vertical_slab")) {
            event.setCancelled(true);
        }
    }
}
