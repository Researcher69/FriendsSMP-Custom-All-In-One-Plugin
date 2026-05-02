package com.friendssmp.manager;

import com.friendssmp.storage.Database;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerRegistryManager {
    private final Database database;
    private final Map<UUID, KnownPlayer> knownPlayers = new ConcurrentHashMap<>();

    public PlayerRegistryManager(JavaPlugin plugin, Database database) {
        this.database = database;
    }

    public void load() {
        try (var connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT player_uuid, player_name, last_seen_ms FROM known_players")) {
            while (resultSet.next()) {
                knownPlayers.put(UUID.fromString(resultSet.getString("player_uuid")),
                        new KnownPlayer(UUID.fromString(resultSet.getString("player_uuid")), resultSet.getString("player_name"), resultSet.getLong("last_seen_ms")));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load known players.", exception);
        }
    }

    public void recordJoin(Player player) {
        KnownPlayer knownPlayer = new KnownPlayer(player.getUniqueId(), player.getName(), System.currentTimeMillis());
        knownPlayers.put(player.getUniqueId(), knownPlayer);
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO known_players(player_uuid, player_name, last_seen_ms)
                    VALUES(?, ?, ?)
                    ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name, last_seen_ms = excluded.last_seen_ms
                    """)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.setLong(3, knownPlayer.lastSeenMs());
                statement.executeUpdate();
            }
        });
    }

    public void addOffline(OfflinePlayer player) {
        if (player.getName() == null) {
            return;
        }
        knownPlayers.putIfAbsent(player.getUniqueId(), new KnownPlayer(player.getUniqueId(), player.getName(), 0L));
    }

    public List<KnownPlayer> allPlayers() {
        List<KnownPlayer> players = new ArrayList<>(knownPlayers.values());
        players.sort(Comparator.comparing(KnownPlayer::name, String.CASE_INSENSITIVE_ORDER));
        return players;
    }

    public record KnownPlayer(UUID uuid, String name, long lastSeenMs) {
    }
}
