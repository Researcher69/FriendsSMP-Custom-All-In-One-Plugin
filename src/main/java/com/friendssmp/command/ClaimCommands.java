package com.friendssmp.command;

import com.friendssmp.gui.ClaimGUIManager;
import com.friendssmp.manager.ClaimManager;
import com.friendssmp.manager.ClaimWandManager;
import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ClaimCommands implements CommandExecutor {
    private final ClaimManager claimManager;
    private final ClaimWandManager claimWandManager;
    private final ClaimGUIManager claimGUIManager;

    public ClaimCommands(ClaimManager claimManager, ClaimWandManager claimWandManager, ClaimGUIManager claimGUIManager) {
        this.claimManager = claimManager;
        this.claimWandManager = claimWandManager;
        this.claimGUIManager = claimGUIManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&cOnly players can use claim commands.");
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "claim" -> claimWandManager.giveWand(player);
            case "unclaim" -> Message.send(player, claimManager.unclaim(player) ? "&aUnclaimed this region." : "&cYou do not own this claim.");
            case "claims" -> claimGUIManager.openClaims(player);
            case "trust" -> trust(player, args);
            case "untrust" -> untrust(player, args);
            default -> Message.send(player, "&e/claim, /claims, /trust <player>, /untrust <player>");
        }
        return true;
    }

    private void trust(Player player, String[] args) {
        if (args.length < 1) {
            Message.send(player, "&cUsage: /trust <player>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        Message.send(player, claimManager.trust(player, target) ? "&aTrusted &f" + args[0] + "&a in this claim." : "&cStand in a claim you own.");
    }

    private void untrust(Player player, String[] args) {
        if (args.length < 1) {
            Message.send(player, "&cUsage: /untrust <player>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        Message.send(player, claimManager.untrust(player, target) ? "&aRemoved trust for &f" + args[0] + "&a." : "&cStand in a claim you own.");
    }
}
