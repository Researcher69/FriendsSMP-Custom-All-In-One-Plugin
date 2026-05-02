package com.friendssmp.manager;

import com.friendssmp.model.Claim;
import com.friendssmp.storage.Database;
import org.bukkit.Location;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClaimManager {
    private final JavaPlugin plugin;
    private final Database database;
    private final Map<Integer, Claim> claims = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> claimCounts = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public ClaimManager(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void load() {
        claims.clear();
        claimCounts.clear();
        try (var connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM claim_regions")) {
            int maxId = 0;
            while (resultSet.next()) {
                Claim claim = new Claim(
                        resultSet.getInt("id"),
                        resultSet.getString("world"),
                        UUID.fromString(resultSet.getString("owner_uuid")),
                        resultSet.getString("owner_name"),
                        resultSet.getString("plot_name"),
                        resultSet.getInt("min_x"),
                        resultSet.getInt("min_y"),
                        resultSet.getInt("min_z"),
                        resultSet.getInt("max_x"),
                        resultSet.getInt("max_y"),
                        resultSet.getInt("max_z"),
                        resultSet.getInt("entity_damage_enabled") == 1);
                claims.put(claim.id(), claim);
                claimCounts.merge(claim.ownerUuid(), 1, Integer::sum);
                maxId = Math.max(maxId, claim.id());
            }
            nextId.set(maxId + 1);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load claims.", exception);
        }

        try (var connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM claim_region_trust")) {
            while (resultSet.next()) {
                Claim claim = claims.get(resultSet.getInt("claim_id"));
                if (claim != null) {
                    claim.trusted().add(UUID.fromString(resultSet.getString("trusted_uuid")));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load claim trust.", exception);
        }
    }

    public Optional<Claim> at(Location location) {
        return claims.values().stream()
                .filter(claim -> claim.contains(location))
                .findFirst();
    }

    public Optional<Claim> byId(int claimId) {
        return Optional.ofNullable(claims.get(claimId));
    }

    public boolean canInteract(Player player, Location location) {
        if (player.hasPermission("friendssmp.admin")) {
            return true;
        }
        return at(location).map(claim -> claim.canBuild(player.getUniqueId())).orElse(true);
    }

    public ClaimResult createRegion(Player player, Location first, Location second) {
        if (first.getWorld() == null || second.getWorld() == null || !first.getWorld().equals(second.getWorld())) {
            return ClaimResult.DIFFERENT_WORLDS;
        }
        int minX = Math.min(first.getBlockX(), second.getBlockX());
        int minY = Math.min(first.getBlockY(), second.getBlockY());
        int minZ = Math.min(first.getBlockZ(), second.getBlockZ());
        int maxX = Math.max(first.getBlockX(), second.getBlockX());
        int maxY = Math.max(first.getBlockY(), second.getBlockY());
        int maxZ = Math.max(first.getBlockZ(), second.getBlockZ());
        int maxSize = plugin.getConfig().getInt("claims.max-size", 500);
        if (maxX - minX + 1 > maxSize || maxZ - minZ + 1 > maxSize) {
            return ClaimResult.TOO_LARGE;
        }
        int maxClaims = plugin.getConfig().getInt("claims.max-per-player", 2);
        if (claimCounts.getOrDefault(player.getUniqueId(), 0) >= maxClaims) {
            return ClaimResult.LIMIT_REACHED;
        }
        String world = first.getWorld().getName();
        if (claims.values().stream().anyMatch(claim -> claim.overlaps(world, minX, minZ, maxX, maxZ))) {
            return ClaimResult.OVERLAPS;
        }

        int id = nextId.getAndIncrement();
        String plotName = player.getName() + "'s Claim " + (claimCounts.getOrDefault(player.getUniqueId(), 0) + 1);
        Claim claim = new Claim(id, world, player.getUniqueId(), player.getName(), plotName, minX, minY, minZ, maxX, maxY, maxZ, false);
        claims.put(id, claim);
        claimCounts.merge(player.getUniqueId(), 1, Integer::sum);
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO claim_regions(id, world, owner_uuid, owner_name, plot_name, min_x, min_y, min_z, max_x, max_y, max_z, entity_damage_enabled)
                    VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setInt(1, id);
                statement.setString(2, world);
                statement.setString(3, player.getUniqueId().toString());
                statement.setString(4, player.getName());
                statement.setString(5, plotName);
                statement.setInt(6, minX);
                statement.setInt(7, minY);
                statement.setInt(8, minZ);
                statement.setInt(9, maxX);
                statement.setInt(10, maxY);
                statement.setInt(11, maxZ);
                statement.setInt(12, 0);
                statement.executeUpdate();
            }
        });
        return ClaimResult.SUCCESS;
    }

    public boolean unclaim(Player player) {
        Optional<Claim> optionalClaim = at(player.getLocation());
        if (optionalClaim.isEmpty()) {
            return false;
        }
        Claim claim = optionalClaim.get();
        if (!claim.ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("friendssmp.admin")) {
            return false;
        }
        claims.remove(claim.id());
        claimCounts.computeIfPresent(claim.ownerUuid(), (uuid, count) -> Math.max(0, count - 1));
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM claim_regions WHERE id = ?")) {
                statement.setInt(1, claim.id());
                statement.executeUpdate();
            }
        });
        return true;
    }

    public boolean trust(Player owner, OfflinePlayer target) {
        Optional<Claim> optionalClaim = at(owner.getLocation());
        if (optionalClaim.isEmpty()) {
            return false;
        }
        return trust(optionalClaim.get(), owner, target);
    }

    public boolean trust(Claim claim, Player owner, OfflinePlayer target) {
        if (!claim.ownerUuid().equals(owner.getUniqueId())) {
            return false;
        }
        claim.trusted().add(target.getUniqueId());
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO claim_region_trust(claim_id, trusted_uuid, trusted_name) VALUES(?, ?, ?)")) {
                statement.setInt(1, claim.id());
                statement.setString(2, target.getUniqueId().toString());
                statement.setString(3, target.getName() == null ? target.getUniqueId().toString() : target.getName());
                statement.executeUpdate();
            }
        });
        return true;
    }

    public boolean untrust(Player owner, OfflinePlayer target) {
        Optional<Claim> optionalClaim = at(owner.getLocation());
        if (optionalClaim.isEmpty()) {
            return false;
        }
        return untrust(optionalClaim.get(), owner, target);
    }

    public boolean untrust(Claim claim, Player owner, OfflinePlayer target) {
        if (!claim.ownerUuid().equals(owner.getUniqueId())) {
            return false;
        }
        claim.trusted().remove(target.getUniqueId());
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM claim_region_trust WHERE claim_id = ? AND trusted_uuid = ?")) {
                statement.setInt(1, claim.id());
                statement.setString(2, target.getUniqueId().toString());
                statement.executeUpdate();
            }
        });
        return true;
    }

    public List<Claim> claimsOf(UUID playerUuid) {
        List<Claim> playerClaims = new ArrayList<>();
        claims.values().forEach(claim -> {
            if (claim.ownerUuid().equals(playerUuid)) {
                playerClaims.add(claim);
            }
        });
        playerClaims.sort(Comparator.comparing(Claim::world).thenComparingInt(Claim::minX).thenComparingInt(Claim::minZ));
        return playerClaims;
    }

    public enum ClaimResult {
        SUCCESS,
        DIFFERENT_WORLDS,
        TOO_LARGE,
        LIMIT_REACHED,
        OVERLAPS
    }
}
