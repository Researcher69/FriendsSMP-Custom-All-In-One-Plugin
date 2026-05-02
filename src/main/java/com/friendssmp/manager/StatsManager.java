package com.friendssmp.manager;

import com.friendssmp.model.PlayerStats;
import com.friendssmp.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatsManager {
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final Database database;
    private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    public StatsManager(org.bukkit.plugin.java.JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        Bukkit.getScheduler().runTaskTimer(plugin, this::flushOnlinePlaytimeAndContinue, 20L * 300L, 20L * 300L);
    }

    public void loadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            handleJoin(player);
        }
    }

    public void handleJoin(Player player) {
        joinTimes.put(player.getUniqueId(), System.currentTimeMillis());
        statsCache.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerStats(uuid, player.getName(), 0, 0, 0)).name(player.getName());
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO stats(player_uuid, player_name) VALUES(?, ?) ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name")) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.executeUpdate();
            }
            loadPlayerFromDatabase(player.getUniqueId(), player.getName());
        });
    }

    public void handleQuit(Player player) {
        flushPlaytime(player.getUniqueId(), false);
    }

    public void addKill(Player player) {
        PlayerStats stats = statsCache.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerStats(uuid, player.getName(), 0, 0, 0));
        stats.name(player.getName());
        stats.addKill();
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE stats SET player_name = ?, kills = kills + 1 WHERE player_uuid = ?")) {
                statement.setString(1, player.getName());
                statement.setString(2, player.getUniqueId().toString());
                statement.executeUpdate();
            }
        });
    }

    public void addDeath(Player player) {
        PlayerStats stats = statsCache.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerStats(uuid, player.getName(), 0, 0, 0));
        stats.name(player.getName());
        stats.addDeath();
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE stats SET player_name = ?, deaths = deaths + 1 WHERE player_uuid = ?")) {
                statement.setString(1, player.getName());
                statement.setString(2, player.getUniqueId().toString());
                statement.executeUpdate();
            }
        });
    }

    public void flushOnlinePlaytime() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            flushPlaytime(player.getUniqueId(), false);
        }
    }

    public void flushOnlinePlaytimeAndContinue() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            flushPlaytime(player.getUniqueId(), true);
        }
    }

    public Optional<PlayerStats> statsFor(OfflinePlayer offlinePlayer) {
        PlayerStats cached = statsCache.get(offlinePlayer.getUniqueId());
        if (cached != null) {
            long currentSession = currentSessionMinutes(offlinePlayer.getUniqueId());
            return Optional.of(new PlayerStats(cached.uuid(), cached.name(), cached.kills(), cached.deaths(), cached.playtimeMinutes() + currentSession));
        }
        try (var connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM stats WHERE player_uuid = ?")) {
            statement.setString(1, offlinePlayer.getUniqueId().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    PlayerStats stats = readStats(resultSet);
                    statsCache.put(stats.uuid(), stats);
                    return Optional.of(stats);
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public List<PlayerStats> top(String column) {
        if (!List.of("kills", "deaths", "playtime_minutes").contains(column)) {
            return List.of();
        }
        List<PlayerStats> rows = new ArrayList<>();
        try (var connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM stats ORDER BY " + column + " DESC LIMIT 10");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.add(readStats(resultSet));
            }
        } catch (Exception ignored) {
        }
        rows.sort(switch (column) {
            case "kills" -> Comparator.comparingInt(PlayerStats::kills).reversed();
            case "deaths" -> Comparator.comparingInt(PlayerStats::deaths).reversed();
            default -> Comparator.comparingLong(PlayerStats::playtimeMinutes).reversed();
        });
        return rows;
    }

    private void flushPlaytime(UUID uuid, boolean keepTracking) {
        Long joined = joinTimes.remove(uuid);
        if (joined == null) {
            return;
        }
        long minutes = Math.max(0, (System.currentTimeMillis() - joined) / 60000L);
        if (minutes == 0) {
            if (keepTracking) {
                joinTimes.put(uuid, joined);
            }
            return;
        }
        if (keepTracking) {
            joinTimes.put(uuid, System.currentTimeMillis());
        }
        PlayerStats stats = statsCache.get(uuid);
        if (stats != null) {
            stats.addPlaytimeMinutes(minutes);
        }
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE stats SET playtime_minutes = playtime_minutes + ? WHERE player_uuid = ?")) {
                statement.setLong(1, minutes);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            }
        });
    }

    private long currentSessionMinutes(UUID uuid) {
        Long joined = joinTimes.get(uuid);
        return joined == null ? 0 : Math.max(0, (System.currentTimeMillis() - joined) / 60000L);
    }

    private void loadPlayerFromDatabase(UUID uuid, String fallbackName) throws java.sql.SQLException {
        try (var connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM stats WHERE player_uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    statsCache.put(uuid, readStats(resultSet));
                } else {
                    statsCache.put(uuid, new PlayerStats(uuid, fallbackName, 0, 0, 0));
                }
            }
        }
    }

    private PlayerStats readStats(ResultSet resultSet) throws java.sql.SQLException {
        return new PlayerStats(
                UUID.fromString(resultSet.getString("player_uuid")),
                resultSet.getString("player_name"),
                resultSet.getInt("kills"),
                resultSet.getInt("deaths"),
                resultSet.getLong("playtime_minutes"));
    }
}
