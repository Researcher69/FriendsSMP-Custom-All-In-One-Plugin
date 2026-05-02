package com.friendssmp.manager;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AdminManager {
    private final JavaPlugin plugin;
    private final Set<UUID> admins = new HashSet<>();

    public AdminManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        admins.clear();
        for (String value : plugin.getConfig().getStringList("admins")) {
            try {
                admins.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public boolean isAdmin(CommandSender sender) {
        if (sender.hasPermission("friendssmp.admin")) {
            return true;
        }
        return sender instanceof Player player && admins.contains(player.getUniqueId());
    }

    public void addAdmin(OfflinePlayer player) {
        admins.add(player.getUniqueId());
        save();
    }

    public void removeAdmin(OfflinePlayer player) {
        admins.remove(player.getUniqueId());
        save();
    }

    private void save() {
        plugin.getConfig().set("admins", admins.stream().map(UUID::toString).sorted().toList());
        plugin.saveConfig();
    }
}
