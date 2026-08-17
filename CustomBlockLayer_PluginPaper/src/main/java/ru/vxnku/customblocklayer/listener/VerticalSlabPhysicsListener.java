package ru.vxnku.customblocklayer.listener;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

public class VerticalSlabPhysicsListener implements Listener {
    private final CustomBlockLayerPlugin plugin;
    private static final double CHECK_RADIUS = 1.25;

    public VerticalSlabPhysicsListener(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSuffocationDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION && event.getEntity() instanceof Player player) {
            if (isNearVerticalSlab(player.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isNearVerticalSlab(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        int minX = (int) Math.floor(loc.getX() - CHECK_RADIUS);
        int maxX = (int) Math.floor(loc.getX() + CHECK_RADIUS);
        int minY = (int) Math.floor(loc.getY() - CHECK_RADIUS);
        int maxY = (int) Math.floor(loc.getY() + CHECK_RADIUS);
        int minZ = (int) Math.floor(loc.getZ() - CHECK_RADIUS);
        int maxZ = (int) Math.floor(loc.getZ() + CHECK_RADIUS);

        Location checkLoc = new Location(world, 0, 0, 0);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    checkLoc.set(x, y, z);
                    String customId = plugin.getStorageManager().getStorage().getBlock(checkLoc);
                    if (customId != null && customId.contains("vertical_slab")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
