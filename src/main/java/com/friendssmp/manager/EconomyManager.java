package com.friendssmp.manager;

import com.friendssmp.storage.Database;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class EconomyManager {
    private final JavaPlugin plugin;
    private final Database database;
    private final Map<UUID, Integer> balances = new ConcurrentHashMap<>();
    private final Map<UUID, Object> locks = new ConcurrentHashMap<>();

    public EconomyManager(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void loadOnline(Player player) {
        database.executeAsync(connection -> {
            int balance = 0;
            try (PreparedStatement select = connection.prepareStatement("SELECT balance FROM economy_balances WHERE player_uuid = ?")) {
                select.setString(1, player.getUniqueId().toString());
                try (ResultSet resultSet = select.executeQuery()) {
                    if (resultSet.next()) {
                        balance = resultSet.getInt("balance");
                    }
                }
            }
            balances.put(player.getUniqueId(), Math.max(0, balance));
            upsertBalance(connection, player.getUniqueId(), player.getName(), balance);
        });
    }

    public int balance(UUID uuid) {
        return balances.getOrDefault(uuid, 0);
    }

    public CompletableFuture<Integer> add(Player player, int amount) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        if (amount < 0) {
            future.completeExceptionally(new IllegalArgumentException("Amount must be positive."));
            return future;
        }
        Object lock = lock(player.getUniqueId());
        int newBalance;
        synchronized (lock) {
            newBalance = balances.getOrDefault(player.getUniqueId(), 0) + amount;
            balances.put(player.getUniqueId(), newBalance);
        }
        int finalBalance = newBalance;
        database.executeAsync(connection -> {
            upsertBalance(connection, player.getUniqueId(), player.getName(), finalBalance);
            future.complete(finalBalance);
        });
        return future;
    }

    public CompletableFuture<Boolean> withdraw(Player player, int amount) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (amount <= 0) {
            future.complete(false);
            return future;
        }
        Object lock = lock(player.getUniqueId());
        int newBalance;
        synchronized (lock) {
            int current = balances.getOrDefault(player.getUniqueId(), 0);
            if (current < amount) {
                future.complete(false);
                return future;
            }
            newBalance = current - amount;
            balances.put(player.getUniqueId(), newBalance);
        }
        int finalBalance = newBalance;
        database.executeAsync(connection -> {
            upsertBalance(connection, player.getUniqueId(), player.getName(), finalBalance);
            future.complete(true);
        });
        return future;
    }

    public CompletableFuture<TransferResult> transfer(Player sender, Player target, int amount) {
        CompletableFuture<TransferResult> future = new CompletableFuture<>();
        if (amount <= 0 || sender.getUniqueId().equals(target.getUniqueId())) {
            future.complete(TransferResult.INVALID);
            return future;
        }
        UUID first = sender.getUniqueId().compareTo(target.getUniqueId()) <= 0 ? sender.getUniqueId() : target.getUniqueId();
        UUID second = first.equals(sender.getUniqueId()) ? target.getUniqueId() : sender.getUniqueId();
        Object firstLock = lock(first);
        Object secondLock = lock(second);
        int senderBalance;
        int targetBalance;
        synchronized (firstLock) {
            synchronized (secondLock) {
                senderBalance = balances.getOrDefault(sender.getUniqueId(), 0);
                if (senderBalance < amount) {
                    future.complete(TransferResult.NOT_ENOUGH_COINS);
                    return future;
                }
                senderBalance -= amount;
                targetBalance = balances.getOrDefault(target.getUniqueId(), 0) + amount;
                balances.put(sender.getUniqueId(), senderBalance);
                balances.put(target.getUniqueId(), targetBalance);
            }
        }
        int finalSenderBalance = senderBalance;
        int finalTargetBalance = targetBalance;
        database.executeAsync(connection -> {
            connection.setAutoCommit(false);
            upsertBalance(connection, sender.getUniqueId(), sender.getName(), finalSenderBalance);
            upsertBalance(connection, target.getUniqueId(), target.getName(), finalTargetBalance);
            connection.commit();
            future.complete(TransferResult.SUCCESS);
        });
        return future;
    }

    private Object lock(UUID uuid) {
        return locks.computeIfAbsent(uuid, ignored -> new Object());
    }

    private void upsertBalance(java.sql.Connection connection, UUID uuid, String name, int balance) throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_balances(player_uuid, player_name, balance)
                VALUES(?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name, balance = excluded.balance
                """)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setInt(3, Math.max(0, balance));
            statement.executeUpdate();
        }
    }

    public enum TransferResult {
        SUCCESS,
        NOT_ENOUGH_COINS,
        INVALID
    }
}
