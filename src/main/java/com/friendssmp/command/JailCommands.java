package com.friendssmp.command;

import com.friendssmp.manager.AdminManager;
import com.friendssmp.manager.JailManager;
import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class JailCommands implements CommandExecutor {
    private final AdminManager adminManager;
    private final JailManager jailManager;

    public JailCommands(AdminManager adminManager, JailManager jailManager) {
        this.adminManager = adminManager;
        this.jailManager = jailManager;
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
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Message.send(sender, "&cThat player must be online.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("jail")) {
            jailManager.jail(target, "Admin action");
            Message.send(sender, "&aJailed &f" + target.getName() + "&a.");
        } else {
            Message.send(sender, jailManager.unjail(target) ? "&aUnjailed &f" + target.getName() + "&a." : "&cThat player is not jailed.");
        }
        return true;
    }
}
