package ru.vxnku.customblocklayer.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;

public class CBLClientNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger("CustomBlockLayer-Net");
    public static final String PROTOCOL_VERSION = "0.1";

    public static void init() {
        // Register Payload Types
        PayloadTypeRegistry.playS2C().register(CBLPayloads.SetBlockPayload.ID, CBLPayloads.SetBlockPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CBLPayloads.ClearBlockPayload.ID, CBLPayloads.ClearBlockPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CBLPayloads.ChunkBlocksPayload.ID, CBLPayloads.ChunkBlocksPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CBLPayloads.HandshakePayload.ID, CBLPayloads.HandshakePayload.CODEC);

        // Register Global Packet Receivers (S2C)
        ClientPlayNetworking.registerGlobalReceiver(CBLPayloads.SetBlockPayload.ID, (payload, context) -> {
            LOGGER.info("[CBL-Client] Received SetBlockPayload at {}: {}", payload.pos(), payload.customId());
            context.client().execute(() -> {
                BlockOverrideCache.set(payload.pos(), payload.customId());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(CBLPayloads.ClearBlockPayload.ID, (payload, context) -> {
            LOGGER.info("[CBL-Client] Received ClearBlockPayload at {}", payload.pos());
            context.client().execute(() -> {
                BlockOverrideCache.remove(payload.pos());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(CBLPayloads.ChunkBlocksPayload.ID, (payload, context) -> {
            LOGGER.info("[CBL-Client] Received ChunkBlocksPayload for chunk ({}, {}) with {} blocks", payload.chunkX(), payload.chunkZ(), payload.blocks().size());
            context.client().execute(() -> {
                BlockOverrideCache.setChunkBlocks(payload.chunkX(), payload.chunkZ(), payload.blocks());
            });
        });

        // Lifecycle Events
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            BlockOverrideCache.onChunkUnload(chunk.getPos().x, chunk.getPos().z);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("[CBL-Client] Disconnected, clearing BlockOverrideCache");
            BlockOverrideCache.clear();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ru.vxnku.customblocklayer.render.cit.CitModelBridge.logConnectionHandshake();
            if (ClientPlayNetworking.canSend(CBLPayloads.HandshakePayload.ID)) {
                sender.sendPacket(new CBLPayloads.HandshakePayload(PROTOCOL_VERSION));
                LOGGER.info("[CBL-Client] Sent CBL Handshake packet to server.");
            }
        });

        LOGGER.info("[CBL-Client] CustomBlockLayer Client Networking initialized.");
    }
}
