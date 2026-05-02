package com.friendssmp.model;

import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Claim {
    private final int id;
    private final String world;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String plotName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final Set<UUID> trusted = ConcurrentHashMap.newKeySet();
    private volatile boolean entityDamageEnabled;

    public Claim(int id, String world, UUID ownerUuid, String ownerName, String plotName,
                 int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean entityDamageEnabled) {
        this.id = id;
        this.world = world;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.plotName = plotName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.entityDamageEnabled = entityDamageEnabled;
    }

    public int id() {
        return id;
    }

    public String world() {
        return world;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public String ownerName() {
        return ownerName;
    }

    public String plotName() {
        return plotName;
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxY() {
        return maxY;
    }

    public int maxZ() {
        return maxZ;
    }

    public int widthX() {
        return maxX - minX + 1;
    }

    public int widthZ() {
        return maxZ - minZ + 1;
    }

    public Set<UUID> trusted() {
        return trusted;
    }

    public boolean entityDamageEnabled() {
        return entityDamageEnabled;
    }

    public void entityDamageEnabled(boolean entityDamageEnabled) {
        this.entityDamageEnabled = entityDamageEnabled;
    }

    public boolean contains(Location location) {
        return location.getWorld() != null
                && world.equals(location.getWorld().getName())
                && location.getBlockX() >= minX
                && location.getBlockX() <= maxX
                && location.getBlockY() >= minY
                && location.getBlockY() <= maxY
                && location.getBlockZ() >= minZ
                && location.getBlockZ() <= maxZ;
    }

    public boolean overlaps(String otherWorld, int otherMinX, int otherMinZ, int otherMaxX, int otherMaxZ) {
        return world.equals(otherWorld)
                && minX <= otherMaxX
                && maxX >= otherMinX
                && minZ <= otherMaxZ
                && maxZ >= otherMinZ;
    }

    public boolean canBuild(UUID playerUuid) {
        return ownerUuid.equals(playerUuid) || trusted.contains(playerUuid);
    }

    public String coordinateSummary() {
        return minX + ", " + minZ + " -> " + maxX + ", " + maxZ;
    }
}
