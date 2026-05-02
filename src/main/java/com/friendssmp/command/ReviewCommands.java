package com.friendssmp.command;

import com.friendssmp.manager.ReviewManager;
import com.friendssmp.util.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReviewCommands implements CommandExecutor {
    private final ReviewManager reviewManager;

    public ReviewCommands(ReviewManager reviewManager) {
        this.reviewManager = reviewManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("appeal")) {
            Message.send(sender, "&eContact admin on Discord: &fmklwde");
            return true;
        }
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("reviews")) {
            reviewManager.openReviews(player);
        } else {
            reviewManager.openReview(player);
        }
        return true;
    }
}
