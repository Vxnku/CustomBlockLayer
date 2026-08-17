package ru.vxnku.customblocklayer.integration.coreprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * SQLite-based history storage for CustomBlockLayer blocks.
 * Records all block placement and break operations to support
 * selective CoreProtect rollbacks and inspector lookups.
 */
public class CBLHistoryStorage {
    private final CustomBlockLayerPlugin plugin;
    private final File dbFile;
    private final String connectionUrl;
    private Connection connection;
    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor();

    public static class HistoryEntry {
        public final long id;
        public final long timestamp;
        public final String world;
        public final int x;
        public final int y;
        public final int z;
        public final int action; // 1 = PLACE, 0 = BREAK
        public final String customId;
        public final String user;

        public HistoryEntry(long id, long timestamp, String world, int x, int y, int z, int action, String customId, String user) {
            this.id = id;
            this.timestamp = timestamp;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.action = action;
            this.customId = customId;
            this.user = user;
        }
    }

    public CBLHistoryStorage(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dbFile = new File(dataFolder, "cbl_history.db");
        this.connectionUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        initDatabase();
    }

    private synchronized void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(connectionUrl);

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS cbl_block_history (" +
                    "   id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "   timestamp INTEGER NOT NULL," +
                    "   world TEXT NOT NULL," +
                    "   x INTEGER NOT NULL," +
                    "   y INTEGER NOT NULL," +
                    "   z INTEGER NOT NULL," +
                    "   action INTEGER NOT NULL," + // 1 = PLACE, 0 = BREAK
                    "   custom_id TEXT NOT NULL," +
                    "   user TEXT NOT NULL" +
                    ");"
                );
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cbl_coords ON cbl_block_history(world, x, y, z);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cbl_time ON cbl_block_history(timestamp);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cbl_custom_id ON cbl_block_history(custom_id);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cbl_user ON cbl_block_history(user);");
            }

            plugin.getLogger().info("[CBL-History] База данных истории кастомных блоков инициализирована: " + dbFile.getName());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[CBL-History] Ошибка инициализации SQLite базы истории: " + e.getMessage(), e);
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(connectionUrl);
        }
        return connection;
    }

    /**
     * Records a placement or break action asynchronously.
     */
    public void logAction(long timestamp, @NotNull String world, int x, int y, int z, int action, @NotNull String customId, @NotNull String user) {
        asyncExecutor.submit(() -> {
            String sql = "INSERT INTO cbl_block_history (timestamp, world, x, y, z, action, custom_id, user) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setLong(1, timestamp);
                ps.setString(2, world);
                ps.setInt(3, x);
                ps.setInt(4, y);
                ps.setInt(5, z);
                ps.setInt(6, action);
                ps.setString(7, customId);
                ps.setString(8, user);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("[CBL-History] Ошибка записи действия в историю: " + e.getMessage());
            }
        });
    }

    /**
     * Gets the latest history entries at the given block coordinates.
     */
    @NotNull
    public List<HistoryEntry> getHistoryAt(@NotNull String world, int x, int y, int z, int limit) {
        List<HistoryEntry> entries = new ArrayList<>();
        String sql = "SELECT id, timestamp, world, x, y, z, action, custom_id, user FROM cbl_block_history " +
                     "WHERE world = ? AND x = ? AND y = ? AND z = ? ORDER BY timestamp DESC, id DESC LIMIT ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.setInt(5, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new HistoryEntry(
                        rs.getLong("id"),
                        rs.getLong("timestamp"),
                        rs.getString("world"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getInt("action"),
                        rs.getString("custom_id"),
                        rs.getString("user")
                    ));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CBL-History] Ошибка запроса истории блоков: " + e.getMessage());
        }
        return entries;
    }

    /**
     * Finds entries for rollback/restore in a given region and time frame.
     */
    @NotNull
    public List<HistoryEntry> findEntriesForRollback(@NotNull String world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                                                   long minTimestamp, @Nullable String userFilter, @Nullable String customIdFilter) {
        List<HistoryEntry> entries = new ArrayList<>();
        StringBuilder query = new StringBuilder(
            "SELECT id, timestamp, world, x, y, z, action, custom_id, user FROM cbl_block_history " +
            "WHERE world = ? AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ? AND timestamp >= ?"
        );

        if (userFilter != null && !userFilter.isEmpty()) {
            query.append(" AND LOWER(user) = ?");
        }
        if (customIdFilter != null && !customIdFilter.isEmpty() && !customIdFilter.equals("*")) {
            query.append(" AND LOWER(custom_id) = ?");
        }
        query.append(" ORDER BY timestamp DESC, id DESC");

        try (PreparedStatement ps = getConnection().prepareStatement(query.toString())) {
            int idx = 1;
            ps.setString(idx++, world);
            ps.setInt(idx++, minX);
            ps.setInt(idx++, maxX);
            ps.setInt(idx++, minY);
            ps.setInt(idx++, maxY);
            ps.setInt(idx++, minZ);
            ps.setInt(idx++, maxZ);
            ps.setLong(idx++, minTimestamp);

            if (userFilter != null && !userFilter.isEmpty()) {
                ps.setString(idx++, userFilter.toLowerCase());
            }
            if (customIdFilter != null && !customIdFilter.isEmpty() && !customIdFilter.equals("*")) {
                ps.setString(idx++, customIdFilter.toLowerCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new HistoryEntry(
                        rs.getLong("id"),
                        rs.getLong("timestamp"),
                        rs.getString("world"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getInt("action"),
                        rs.getString("custom_id"),
                        rs.getString("user")
                    ));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CBL-History] Ошибка выборки истории для отката: " + e.getMessage());
        }
        return entries;
    }

    public synchronized void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception ignored) {}
    }
}
