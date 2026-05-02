package com.friendssmp.command;

import com.friendssmp.manager.StatsManager;
import com.friendssmp.model.PlayerStats;
import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class StatsCommands implements CommandExecutor, TabCompleter {
    private final StatsManager statsManager;

    public StatsCommands(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("stats")) {
            OfflinePlayer target = args.length == 0 && sender instanceof org.bukkit.entity.Player player ? player : Bukkit.getOfflinePlayer(args[0]);
            statsManager.statsFor(target).ifPresentOrElse(
                    stats -> sendStats(sender, stats),
                    () -> Message.send(sender, "&cNo stats found for that player."));
            return true;
        }
        if (args.length == 0) {
            Message.send(sender, "&cUsage: /top <kills|deaths|playtime>");
            return true;
        }
        String column = switch (args[0].toLowerCase()) {
            case "kills" -> "kills";
            case "deaths" -> "deaths";
            case "playtime" -> "playtime_minutes";
            default -> "";
        };
        if (column.isEmpty()) {
            Message.send(sender, "&cUsage: /top <kills|deaths|playtime>");
            return true;
        }
        sendTop(sender, args[0].toLowerCase(), column);
        return true;
    }

    private void sendStats(CommandSender sender, PlayerStats stats) {
        Message.send(sender, "&aStats for &f" + stats.name());
        Message.send(sender, "&7Kills: &f" + stats.kills() + " &7Deaths: &f" + stats.deaths() + " &7Playtime: &f" + stats.playtimeMinutes() + "m");
    }

    private void sendTop(CommandSender sender, String label, String column) {
        List<PlayerStats> top = statsManager.top(column);
        Message.send(sender, "&aTop " + label + ":");
        for (int i = 0; i < top.size(); i++) {
            PlayerStats stats = top.get(i);
            long value = switch (column) {
                case "kills" -> stats.kills();
                case "deaths" -> stats.deaths();
                default -> stats.playtimeMinutes();
            };
            Message.send(sender, "&e#" + (i + 1) + " &f" + stats.name() + " &7- &a" + value + (column.equals("playtime_minutes") ? "m" : ""));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("top") && args.length == 1) {
            return List.of("kills", "deaths", "playtime").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
