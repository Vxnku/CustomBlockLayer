package ru.vxnku.customblocklayer.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PistonMoveListener implements Listener {
    private final CustomBlockLayerPlugin plugin;

    public PistonMoveListener(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        handlePistonMovement(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePistonMovement(event.getBlocks(), event.getDirection());
    }

    private void handlePistonMovement(List<Block> movedBlocks, BlockFace direction) {
        if (movedBlocks.isEmpty()) return;

        Map<Location, String> toMove = new HashMap<>();
        for (Block b : movedBlocks) {
            Location loc = b.getLocation();
            String customId = plugin.getStorageManager().getStorage().getBlock(loc);
            if (customId != null) {
                toMove.put(loc, customId);
            }
        }

        if (toMove.isEmpty()) return;

        // Execute relocation
        for (Map.Entry<Location, String> entry : toMove.entrySet()) {
            Location oldLoc = entry.getKey();
            Location newLoc = oldLoc.clone().add(direction.getModX(), direction.getModY(), direction.getModZ());
            String customId = entry.getValue();

            plugin.getStorageManager().getStorage().removeBlock(oldLoc);
            plugin.getNetworkManager().sendClearBlock(oldLoc);

            plugin.getStorageManager().getStorage().setBlock(newLoc, customId);
            plugin.getNetworkManager().sendSetBlock(newLoc, customId);
        }
    }
}
