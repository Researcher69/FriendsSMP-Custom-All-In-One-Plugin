package com.friendssmp.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class Message {
    private static String prefix = "";

    private Message() {
    }

    public static void init(JavaPlugin plugin) {
        prefix = color(plugin.getConfig().getString("messages.prefix", "&a[FriendsSMP]&r "));
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(prefix + color(message));
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
