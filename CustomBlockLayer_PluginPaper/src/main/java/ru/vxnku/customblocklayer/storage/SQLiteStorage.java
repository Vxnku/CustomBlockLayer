package ru.vxnku.customblocklayer.storage;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SQLiteStorage implements ICBLStorage {
    private final CustomBlockLayerPlugin plugin;
    private Connection connection;

    public SQLiteStorage(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "blocks.db");
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS cbl_blocks (" +
                    "world TEXT NOT NULL, " +
                    "x INT NOT NULL, " +
                    "y INT NOT NULL, " +
                    "z INT NOT NULL, " +
                    "chunk_x INT NOT NULL, " +
                    "chunk_z INT NOT NULL, " +
                    "custom_id TEXT NOT NULL, " +
                    "PRIMARY KEY (world, x, y, z)" +
                    ");");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chunk ON cbl_blocks (world, chunk_x, chunk_z);");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize SQLite storage: " + e.getMessage());
        }
    }

    @Override
    public synchronized void setBlock(@NotNull Location loc, @NotNull String customId) {
        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT OR REPLACE INTO cbl_blocks (world, x, y, z, chunk_x, chunk_z, custom_id) VALUES (?, ?, ?, ?, ?, ?, ?);")) {
            ps.setString(1, loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            ps.setInt(5, loc.getBlockX() >> 4);
            ps.setInt(6, loc.getBlockZ() >> 4);
            ps.setString(7, customId);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("SQLite setBlock error: " + e.getMessage());
        }
    }

    @Override
    public synchronized void removeBlock(@NotNull Location loc) {
        try (PreparedStatement ps = connection.prepareStatement(
            "DELETE FROM cbl_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;")) {
            ps.setString(1, loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("SQLite removeBlock error: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public synchronized String getBlock(@NotNull Location loc) {
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT custom_id FROM cbl_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;")) {
            ps.setString(1, loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("custom_id");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("SQLite getBlock error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean hasBlock(@NotNull Location loc) {
        return getBlock(loc) != null;
    }

    @NotNull
    @Override
    public synchronized Map<Integer, String> getChunkBlocks(@NotNull Chunk chunk) {
        Map<Integer, String> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT x, y, z, custom_id FROM cbl_blocks WHERE world = ? AND chunk_x = ? AND chunk_z = ?;")) {
            ps.setString(1, chunk.getWorld().getName());
            ps.setInt(2, chunk.getX());
            ps.setInt(3, chunk.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    String customId = rs.getString("custom_id");
                    int packed = PDCChunkStorage.packLocalPos(x, y, z);
                    map.put(packed, customId);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("SQLite getChunkBlocks error: " + e.getMessage());
        }
        return map;
    }

    @Override
    public void saveChunk(@NotNull Chunk chunk) {}
    @Override
    public void loadChunk(@NotNull Chunk chunk) {}

    @Override
    public synchronized void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception ignored) {}
    }
}
