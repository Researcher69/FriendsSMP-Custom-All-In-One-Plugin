package com.friendssmp.command;

import com.friendssmp.manager.AdminManager;
import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class AdminCommands implements CommandExecutor {
    private final AdminManager adminManager;

    public AdminCommands(AdminManager adminManager) {
        this.adminManager = adminManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!adminManager.isAdmin(sender)) {
            Message.send(sender, "&cYou do not have permission.");
            return true;
        }
        if (args.length != 1) {
            Message.send(sender, "&cUsage: /" + label + " <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (command.getName().equalsIgnoreCase("setadmin")) {
            adminManager.addAdmin(target);
            Message.send(sender, "&aAdded &f" + args[0] + "&a as a FriendsSMP admin.");
        } else {
            adminManager.removeAdmin(target);
            Message.send(sender, "&aRemoved &f" + args[0] + "&a from FriendsSMP admins.");
        }
        return true;
    }
}
