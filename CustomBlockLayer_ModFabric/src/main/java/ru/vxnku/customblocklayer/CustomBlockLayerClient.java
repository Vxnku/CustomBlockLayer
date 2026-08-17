package ru.vxnku.customblocklayer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.command.CBLClientCommands;
import ru.vxnku.customblocklayer.config.PropertiesResourceReloadListener;
import ru.vxnku.customblocklayer.item.CBLItemGroups;
import ru.vxnku.customblocklayer.network.CBLClientNetworking;
import ru.vxnku.customblocklayer.util.CBLItemHelper;

public class CustomBlockLayerClient implements ClientModInitializer {
    public static final String MOD_ID = "customblocklayer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[CustomBlockLayer] Initializing client mod...");

        // 1. Register Creative Tab
        CBLItemGroups.init();

        // 2. Register Resource Reload Listener for .properties files
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
            .registerReloadListener(new PropertiesResourceReloadListener());

        // 3. Register Network Payloads & Global Handlers
        CBLClientNetworking.init();

        // 4. Register Client Commands (/cbl set, /cbl clear, etc.)
        CBLClientCommands.init();

        // 5. Singleplayer / Client-side instant place prediction
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (CBLItemHelper.isCustomBlock(stack)) {
                String customId = CBLItemHelper.getCustomBlockId(stack);
                BlockPos placedPos = hitResult.getBlockPos().offset(hitResult.getSide());
                // Pre-set in cache (will be confirmed/synced if server sends packet)
                BlockOverrideCache.set(placedPos, customId);
            }
            return ActionResult.PASS;
        });

        LOGGER.info("[CustomBlockLayer] Successfully initialized with Creative Tab, MMB Pick, and Networking!");
    }
}
