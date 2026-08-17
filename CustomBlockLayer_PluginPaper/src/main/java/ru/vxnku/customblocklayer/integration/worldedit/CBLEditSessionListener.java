package ru.vxnku.customblocklayer.integration.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.eventbus.EventHandler;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import org.bukkit.World;

public class CBLEditSessionListener {

    @Subscribe(priority = EventHandler.Priority.NORMAL)
    public void onEditSession(EditSessionEvent event) {
        if (event.getStage() == EditSession.Stage.BEFORE_CHANGE && event.getWorld() != null) {
            BukkitWorld bukkitWorld = BukkitAdapter.asBukkitWorld(event.getWorld());
            if (bukkitWorld != null && bukkitWorld.getWorld() != null) {
                Region selection = null;
                if (event.getActor() != null) {
                    try {
                        LocalSession session = WorldEdit.getInstance().getSessionManager().get(event.getActor());
                        if (session != null) {
                            selection = session.getSelection(event.getWorld());
                        }
                    } catch (Exception ignored) {}
                }

                event.setExtent(new CBLExtent(event.getExtent(), bukkitWorld.getWorld(), selection));
            }
        }
    }
}
