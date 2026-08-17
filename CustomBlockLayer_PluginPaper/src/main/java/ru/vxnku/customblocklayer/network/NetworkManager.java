package ru.vxnku.customblocklayer.network;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.api.events.CBLPacketSendEvent;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class NetworkManager implements PluginMessageListener {
    public static final String CHANNEL_HANDSHAKE = "customblocklayer:handshake";
    public static final String CHANNEL_SET_BLOCK = "customblocklayer:set_block";
    public static final String CHANNEL_CLEAR_BLOCK = "customblocklayer:clear_block";
    public static final String CHANNEL_CHUNK_BLOCKS = "customblocklayer:chunk_blocks";

    private final CustomBlockLayerPlugin plugin;
    private final PlayerSessionManager sessionManager = new PlayerSessionManager();

    public NetworkManager(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
        registerChannels();
    }

    private void registerChannels() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_HANDSHAKE, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_SET_BLOCK);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_CLEAR_BLOCK);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_CHUNK_BLOCKS);
    }

    @NotNull
    public PlayerSessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (CHANNEL_HANDSHAKE.equals(channel)) {
            String version = "unknown";
            try {
                int length = message.length > 0 ? message[0] : 0;
                if (message.length > 1) {
                    version = new String(message, 1, Math.min(length, message.length - 1), StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {}

            sessionManager.registerCBLPlayer(player, version);
            plugin.getLogger().info("[CBL] Игрок " + player.getName() + " подключен с модом CustomBlockLayer (v" + version + ")");

            // Delay sync by 5 ticks to ensure client world initialization is complete
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    sendLoadedChunksAroundPlayer(player);
                }
            }, 5L);
        }
    }

    public void sendSetBlock(@NotNull Location loc, @NotNull String customId) {
        byte[] data = PacketSerializer.serializeSetBlock(loc, customId);
        broadcastPacket(loc, CHANNEL_SET_BLOCK, data);
    }

    public void sendClearBlock(@NotNull Location loc) {
        byte[] data = PacketSerializer.serializeClearBlock(loc);
        broadcastPacket(loc, CHANNEL_CLEAR_BLOCK, data);
    }

    public void sendChunkBlocks(@NotNull Player player, @NotNull Chunk chunk) {
        if (!sessionManager.isCBLPlayer(player)) {
            return;
        }

        Map<Integer, String> blocks = plugin.getStorageManager().getStorage().getChunkBlocks(chunk);
        if (!blocks.isEmpty()) {
            plugin.getLogger().info("[CBL] Отправка " + blocks.size() + " кастомных блоков в чанке (" + chunk.getX() + ", " + chunk.getZ() + ") игроку " + player.getName());
            byte[] data = PacketSerializer.serializeChunkBlocks(chunk.getX(), chunk.getZ(), blocks);
            sendPacket(player, CHANNEL_CHUNK_BLOCKS, data);
        }
    }

    public void sendLoadedChunksAroundPlayer(@NotNull Player player) {
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        int viewDist = Bukkit.getViewDistance();

        int sentCount = 0;
        for (int dx = -viewDist; dx <= viewDist; dx++) {
            for (int dz = -viewDist; dz <= viewDist; dz++) {
                if (player.getWorld().isChunkLoaded(cx + dx, cz + dz)) {
                    Chunk chunk = player.getWorld().getChunkAt(cx + dx, cz + dz);
                    Map<Integer, String> blocks = plugin.getStorageManager().getStorage().getChunkBlocks(chunk);
                    if (!blocks.isEmpty()) {
                        sendChunkBlocks(player, chunk);
                        sentCount++;
                    }
                }
            }
        }
        if (sentCount > 0) {
            plugin.getLogger().info("[CBL] Всего синхронизировано " + sentCount + " чанков с кастомными блоками для игрока " + player.getName());
        }
    }

    private void broadcastPacket(@NotNull Location loc, @NotNull String channel, @NotNull byte[] data) {
        int radiusSq = 128 * 128;
        for (Player p : loc.getWorld().getPlayers()) {
            if (sessionManager.isCBLPlayer(p) && p.getLocation().distanceSquared(loc) <= radiusSq) {
                sendPacket(p, channel, data);
            }
        }
    }

    private void sendPacket(@NotNull Player player, @NotNull String channel, @NotNull byte[] data) {
        CBLPacketSendEvent event = new CBLPacketSendEvent(player, channel, data);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            player.sendPluginMessage(plugin, event.getChannel(), event.getData());
        }
    }

    public void broadcastChunkBlocks(@NotNull Chunk chunk) {
        Map<Integer, String> blocks = plugin.getStorageManager().getStorage().getChunkBlocks(chunk);
        byte[] data = PacketSerializer.serializeChunkBlocks(chunk.getX(), chunk.getZ(), blocks);
        for (Player p : chunk.getWorld().getPlayers()) {
            if (sessionManager.isCBLPlayer(p)) {
                sendPacket(p, CHANNEL_CHUNK_BLOCKS, data);
            }
        }
    }

}
