package ru.vxnku.customblocklayer.api;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.item.CBLItemFactory;
import ru.vxnku.customblocklayer.registry.ServerBlockRegistry;

/**
 * Public static API for other plugins to interact with CustomBlockLayer.
 */
public class CustomBlockLayerAPI {

    public static void setCustomBlock(@NotNull Location location, @NotNull String customId) {
        CustomBlockLayerPlugin.getInstance().getStorageManager().getStorage().setBlock(location, customId);
        CustomBlockLayerPlugin.getInstance().getNetworkManager().sendSetBlock(location, customId);
    }

    public static void removeCustomBlock(@NotNull Location location) {
        CustomBlockLayerPlugin.getInstance().getStorageManager().getStorage().removeBlock(location);
        CustomBlockLayerPlugin.getInstance().getNetworkManager().sendClearBlock(location);
    }

    @Nullable
    public static String getCustomBlock(@NotNull Location location) {
        return CustomBlockLayerPlugin.getInstance().getStorageManager().getStorage().getBlock(location);
    }

    public static boolean isCustomBlock(@NotNull Location location) {
        return CustomBlockLayerPlugin.getInstance().getStorageManager().getStorage().hasBlock(location);
    }

    @NotNull
    public static ItemStack createCustomItem(@NotNull String customId, @NotNull Material baseMaterial, int amount) {
        return CBLItemFactory.createItem(customId, baseMaterial, amount);
    }

    @NotNull
    public static ItemStack createCustomItem(@NotNull String customId) {
        return CBLItemFactory.createItem(customId);
    }

    @Nullable
    public static String getCustomBlockIdFromItem(@Nullable ItemStack item) {
        return CBLItemFactory.getCustomBlockId(item);
    }

    @NotNull
    public static ServerBlockRegistry getBlockRegistry() {
        return CustomBlockLayerPlugin.getInstance().getBlockRegistry();
    }
}
