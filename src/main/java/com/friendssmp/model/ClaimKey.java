package com.friendssmp.model;

import org.bukkit.Chunk;
import org.bukkit.Location;

public record ClaimKey(String world, int chunkX, int chunkZ) {
    public static ClaimKey from(Location location) {
        Chunk chunk = location.getChunk();
        return new ClaimKey(location.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    @Override
    public String toString() {
        return world + " [" + chunkX + ", " + chunkZ + "]";
    }
}
