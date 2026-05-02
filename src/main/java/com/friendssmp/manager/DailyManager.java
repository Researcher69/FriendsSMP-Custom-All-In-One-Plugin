package com.friendssmp.manager;

import com.friendssmp.storage.Database;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class DailyManager {
    private final JavaPlugin plugin;
    private final Database database;
    private final EconomyManager economyManager;

    public DailyManager(JavaPlugin plugin, Database database, EconomyManager economyManager) {
        this.plugin = plugin;
        this.database = database;
        this.economyManager = economyManager;
    }

    public CompletableFuture<DailyResult> claim(Player player) {
        CompletableFuture<DailyResult> future = new CompletableFuture<>();
        database.executeAsync(connection -> {
            long now = System.currentTimeMillis();
            long cooldownMs = Duration.ofHours(plugin.getConfig().getLong("daily.cooldown-hours", 24)).toMillis();
            long resetMs = Duration.ofHours(plugin.getConfig().getLong("daily.streak-reset-hours", 48)).toMillis();
            long lastClaim = 0;
            int streak = 0;
            try (PreparedStatement select = connection.prepareStatement("SELECT last_claim_ms, streak FROM daily_rewards WHERE player_uuid = ?")) {
                select.setString(1, player.getUniqueId().toString());
                try (ResultSet resultSet = select.executeQuery()) {
                    if (resultSet.next()) {
                        lastClaim = resultSet.getLong("last_claim_ms");
                        streak = resultSet.getInt("streak");
                    }
                }
            }
            if (lastClaim > 0 && now - lastClaim < cooldownMs) {
                future.complete(new DailyResult(false, streak, economyManager.balance(player.getUniqueId()), cooldownMs - (now - lastClaim), 0));
                return;
            }
            int newStreak = lastClaim > 0 && now - lastClaim <= resetMs ? streak + 1 : 1;
            int coinsAwarded = coinsFor(newStreak);
            try (PreparedStatement upsert = connection.prepareStatement("""
                    INSERT INTO daily_rewards(player_uuid, player_name, last_claim_ms, streak, coins)
                    VALUES(?, ?, ?, ?, ?)
                    ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name, last_claim_ms = excluded.last_claim_ms, streak = excluded.streak, coins = excluded.coins
                    """)) {
                upsert.setString(1, player.getUniqueId().toString());
                upsert.setString(2, player.getName());
                upsert.setLong(3, now);
                upsert.setInt(4, newStreak);
                upsert.setInt(5, coinsAwarded);
                upsert.executeUpdate();
            }
            economyManager.add(player, coinsAwarded).thenAccept(total -> future.complete(new DailyResult(true, newStreak, total, 0, coinsAwarded)));
        });
        return future;
    }

    private int coinsFor(int streak) {
        if (streak >= 10) {
            return plugin.getConfig().getInt("daily.reward-scaling.day-10-plus", 100);
        }
        if (streak >= 5) {
            return plugin.getConfig().getInt("daily.reward-scaling.day-5", 50);
        }
        return plugin.getConfig().getInt("daily.reward-scaling.day-1", 10) * streak;
    }

    public record DailyResult(boolean claimed, int streak, int totalCoins, long cooldownLeftMs, int coinsAwarded) {
    }
}
