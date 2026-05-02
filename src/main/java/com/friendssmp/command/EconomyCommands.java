package com.friendssmp.command;

import com.friendssmp.manager.EconomyManager;
import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyCommands implements CommandExecutor {
    private final JavaPlugin plugin;
    private final EconomyManager economyManager;

    public EconomyCommands(JavaPlugin plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("balance")) {
            Message.send(player, "&aBalance: &f" + economyManager.balance(player.getUniqueId()) + " coins&a.");
            return true;
        }
        if (args.length != 2) {
            Message.send(player, "&cUsage: /pay <player> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Message.send(player, "&cThat player is not online.");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            Message.send(player, "&cAmount must be a whole number.");
            return true;
        }
        economyManager.transfer(player, target, amount).thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
            switch (result) {
                case SUCCESS -> {
                    Message.send(player, "&aPaid &f" + amount + " coins&a to &f" + target.getName() + "&a.");
                    Message.send(target, "&aReceived &f" + amount + " coins&a from &f" + player.getName() + "&a.");
                }
                case NOT_ENOUGH_COINS -> Message.send(player, "&cNot enough coins.");
                case INVALID -> Message.send(player, "&cInvalid payment.");
            }
        }));
        return true;
    }
}
