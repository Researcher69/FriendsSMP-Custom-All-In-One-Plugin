package com.friendssmp.command;

import com.friendssmp.manager.TeamManager;
import com.friendssmp.util.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TeamChatCommand implements CommandExecutor {
    private final TeamManager teamManager;

    public TeamChatCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&cPlayers only.");
            return true;
        }
        if (args.length == 0) {
            Message.send(player, "&cUsage: /" + label + " <message>");
            return true;
        }
        teamManager.sendTeamChat(player, String.join(" ", args));
        return true;
    }
}
