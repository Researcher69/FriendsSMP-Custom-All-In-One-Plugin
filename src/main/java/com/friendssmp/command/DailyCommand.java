package com.friendssmp.command;

import com.friendssmp.manager.DailyManager;
import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public final class DailyCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final DailyManager dailyManager;

    public DailyCommand(JavaPlugin plugin, DailyManager dailyManager) {
        this.plugin = plugin;
        this.dailyManager = dailyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&cPlayers only.");
            return true;
        }
        dailyManager.claim(player).thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!result.claimed()) {
                long hours = TimeUnit.MILLISECONDS.toHours(result.cooldownLeftMs());
                long minutes = TimeUnit.MILLISECONDS.toMinutes(result.cooldownLeftMs()) % 60;
                Message.send(player, "&cDaily already claimed. Try again in " + hours + "h " + minutes + "m.");
                return;
            }
            Message.send(player, "&aDaily claimed! Streak: &f" + result.streak() + "&a, coins: &f+" + result.coinsAwarded() + "&a (total " + result.totalCoins() + ").");
        }));
        return true;
    }
}
