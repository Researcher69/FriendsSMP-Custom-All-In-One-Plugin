package com.friendssmp.manager;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class EvidenceManager {
    private final JavaPlugin plugin;
    private final File evidenceFolder;
    private final Map<UUID, Deque<String>> recentLocations = new ConcurrentHashMap<>();

    public EvidenceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.evidenceFolder = new File(plugin.getDataFolder(), "evidence");
        if (!evidenceFolder.exists() && !evidenceFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create evidence folder.");
        }
    }

    public void log(UUID uuid, String playerName, Location location, String event, int increase, int suspicion) {
        String coordinates = location.getWorld().getName() + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        String line = "[" + Instant.now() + "] " + coordinates + " | " + event + " | +" + increase + " | level=" + suspicion;
        recentLocations.computeIfAbsent(uuid, ignored -> new ArrayDeque<>()).addLast(coordinates + " " + event);
        Deque<String> recent = recentLocations.get(uuid);
        while (recent.size() > 10) {
            recent.removeFirst();
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> append(playerName, line));
    }

    public Deque<String> recentLocations(UUID uuid) {
        return new ArrayDeque<>(recentLocations.getOrDefault(uuid, new ArrayDeque<>()));
    }

    private void append(String playerName, String line) {
        File file = new File(evidenceFolder, playerName + ".log");
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(line);
            writer.write(System.lineSeparator());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not write evidence log for " + playerName, exception);
        }
    }
}
