package ru.vxnku.customblocklayer.network;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSessionManager {
    private final Set<UUID> cblPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> clientVersions = new ConcurrentHashMap<>();

    public void registerCBLPlayer(@NotNull Player player, @NotNull String version) {
        cblPlayers.add(player.getUniqueId());
        clientVersions.put(player.getUniqueId(), version);
    }

    public void unregisterPlayer(@NotNull Player player) {
        cblPlayers.remove(player.getUniqueId());
        clientVersions.remove(player.getUniqueId());
    }

    public boolean isCBLPlayer(@NotNull Player player) {
        return cblPlayers.contains(player.getUniqueId());
    }

    public String getVersion(@NotNull Player player) {
        return clientVersions.getOrDefault(player.getUniqueId(), "unknown");
    }

    public int getCBLPlayerCount() {
        return cblPlayers.size();
    }
}
