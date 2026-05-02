package com.friendssmp.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class Database {
    private final JavaPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "FriendsSMP-SQLite");
        thread.setDaemon(true);
        return thread;
    });
    private String jdbcUrl;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }
        jdbcUrl = "jdbc:sqlite:" + new File(dataFolder, "friendssmp.db").getAbsolutePath();
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("CREATE TABLE IF NOT EXISTS teams (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL UNIQUE COLLATE NOCASE," +
                    "owner_uuid TEXT NOT NULL," +
                    "home_world TEXT," +
                    "home_x REAL," +
                    "home_y REAL," +
                    "home_z REAL," +
                    "home_yaw REAL," +
                    "home_pitch REAL)");
            statement.execute("CREATE TABLE IF NOT EXISTS team_members (" +
                    "team_id INTEGER NOT NULL," +
                    "player_uuid TEXT NOT NULL PRIMARY KEY," +
                    "player_name TEXT NOT NULL," +
                    "FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE IF NOT EXISTS claims (" +
                    "world TEXT NOT NULL," +
                    "chunk_x INTEGER NOT NULL," +
                    "chunk_z INTEGER NOT NULL," +
                    "owner_uuid TEXT NOT NULL," +
                    "owner_name TEXT NOT NULL," +
                    "PRIMARY KEY(world, chunk_x, chunk_z))");
            statement.execute("CREATE TABLE IF NOT EXISTS claim_trust (" +
                    "world TEXT NOT NULL," +
                    "chunk_x INTEGER NOT NULL," +
                    "chunk_z INTEGER NOT NULL," +
                    "trusted_uuid TEXT NOT NULL," +
                    "trusted_name TEXT NOT NULL," +
                    "PRIMARY KEY(world, chunk_x, chunk_z, trusted_uuid))");
            statement.execute("CREATE TABLE IF NOT EXISTS daily_rewards (" +
                    "player_uuid TEXT PRIMARY KEY," +
                    "player_name TEXT NOT NULL," +
                    "last_claim_ms INTEGER NOT NULL," +
                    "streak INTEGER NOT NULL," +
                    "coins INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS economy_balances (" +
                    "player_uuid TEXT PRIMARY KEY," +
                    "player_name TEXT NOT NULL," +
                    "balance INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS known_players (" +
                    "player_uuid TEXT PRIMARY KEY," +
                    "player_name TEXT NOT NULL," +
                    "last_seen_ms INTEGER NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS claim_regions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT NOT NULL," +
                    "owner_uuid TEXT NOT NULL," +
                    "owner_name TEXT NOT NULL," +
                    "plot_name TEXT NOT NULL," +
                    "min_x INTEGER NOT NULL," +
                    "min_y INTEGER NOT NULL," +
                    "min_z INTEGER NOT NULL," +
                    "max_x INTEGER NOT NULL," +
                    "max_y INTEGER NOT NULL," +
                    "max_z INTEGER NOT NULL," +
                    "entity_damage_enabled INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS claim_region_trust (" +
                    "claim_id INTEGER NOT NULL," +
                    "trusted_uuid TEXT NOT NULL," +
                    "trusted_name TEXT NOT NULL," +
                    "PRIMARY KEY(claim_id, trusted_uuid)," +
                    "FOREIGN KEY(claim_id) REFERENCES claim_regions(id) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE IF NOT EXISTS stats (" +
                    "player_uuid TEXT PRIMARY KEY," +
                    "player_name TEXT NOT NULL," +
                    "kills INTEGER NOT NULL DEFAULT 0," +
                    "deaths INTEGER NOT NULL DEFAULT 0," +
                    "playtime_minutes INTEGER NOT NULL DEFAULT 0)");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize SQLite database.", exception);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
        }
        return connection;
    }

    public void executeAsync(SqlRunnable runnable) {
        executor.execute(() -> {
            try (Connection connection = getConnection()) {
                runnable.run(connection);
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "SQLite task failed.", exception);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("SQLite executor did not finish all queued writes before shutdown.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    public interface SqlRunnable {
        void run(Connection connection) throws SQLException;
    }
}
