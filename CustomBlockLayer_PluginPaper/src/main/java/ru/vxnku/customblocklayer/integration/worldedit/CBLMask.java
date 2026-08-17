package ru.vxnku.customblocklayer.integration.worldedit;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

public class CBLMask extends AbstractMask {
    private final String targetCustomId;
    private World world;

    public CBLMask(@NotNull String targetCustomId) {
        this.targetCustomId = targetCustomId;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        if (world == null) return false;
        Location loc = new Location(world, vector.x(), vector.y(), vector.z());
        String customId = CustomBlockLayerPlugin.getInstance().getStorageManager().getStorage().getBlock(loc);
        if (targetCustomId.equals("*")) {
            return customId != null;
        }
        return targetCustomId.equalsIgnoreCase(customId);
    }
}
