package com.friendssmp.command;

import com.friendssmp.gui.TeamInfoGUI;
import com.friendssmp.manager.TeamManager;
import com.friendssmp.model.Team;
import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TeamCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final TeamInfoGUI teamInfoGUI;

    public TeamCommand(JavaPlugin plugin, TeamManager teamManager, TeamInfoGUI teamInfoGUI) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.teamInfoGUI = teamInfoGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&cPlayers only.");
            return true;
        }
        if (args.length == 0) {
            usage(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> create(player, args);
            case "invite" -> invite(player, args);
            case "accept" -> accept(player);
            case "leave" -> leave(player);
            case "disband" -> disband(player);
            case "info" -> info(player);
            case "sethome" -> setHome(player);
            case "home" -> home(player);
            default -> usage(player);
        }
        return true;
    }

    private void create(Player player, String[] args) {
        if (args.length < 2) {
            Message.send(player, "&cUsage: /team create <name>");
            return;
        }
        if (teamManager.createTeam(player, args[1])) {
            Message.send(player, "&aCreated team &f" + args[1] + "&a.");
        } else {
            Message.send(player, "&cCould not create that team. Use 3-16 letters, numbers, or underscores, and make sure you are not already in a team.");
        }
    }

    private void invite(Player player, String[] args) {
        if (args.length < 2) {
            Message.send(player, "&cUsage: /team invite <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Message.send(player, "&cThat player is not online.");
            return;
        }
        if (teamManager.invite(player, target)) {
            Message.send(player, "&aInvited &f" + target.getName() + "&a.");
            Message.send(target, "&aYou were invited to &f" + player.getName() + "'s&a team. Use &f/team accept&a.");
        } else {
            Message.send(player, "&cCould not invite that player. You must own a team with space available.");
        }
    }

    private void accept(Player player) {
        Optional<Team> accepted = teamManager.accept(player);
        if (accepted.isPresent()) {
            Message.send(player, "&aJoined team &f" + accepted.get().name() + "&a.");
        } else {
            Message.send(player, "&cNo valid team invite found.");
        }
    }

    private void leave(Player player) {
        if (teamManager.leave(player)) {
            Message.send(player, "&aYou left your team.");
        } else {
            Message.send(player, "&cOwners must disband the team instead.");
        }
    }

    private void disband(Player player) {
        if (teamManager.disband(player)) {
            Message.send(player, "&aTeam disbanded.");
        } else {
            Message.send(player, "&cOnly the team owner can disband.");
        }
    }

    private void info(Player player) {
        Optional<Team> optionalTeam = teamManager.teamOf(player.getUniqueId());
        if (optionalTeam.isEmpty()) {
            Message.send(player, "&cYou are not in a team.");
            return;
        }
        teamInfoGUI.open(player, optionalTeam.get());
    }

    private void setHome(Player player) {
        Optional<Team> optionalTeam = teamManager.teamOf(player.getUniqueId());
        if (optionalTeam.isEmpty() || !optionalTeam.get().ownerUuid().equals(player.getUniqueId())) {
            Message.send(player, "&cOnly a team owner can set the team home.");
            return;
        }
        teamManager.setHome(optionalTeam.get(), player.getLocation());
        Message.send(player, "&aTeam home set.");
    }

    private void home(Player player) {
        Optional<Team> optionalTeam = teamManager.teamOf(player.getUniqueId());
        if (optionalTeam.isEmpty() || optionalTeam.get().home() == null) {
            Message.send(player, "&cYour team does not have a home set.");
            return;
        }
        if (teamManager.isHomeOnCooldown(player)) {
            Message.send(player, "&cWait " + teamManager.cooldownSecondsLeft(player) + "s before using team home again.");
            return;
        }
        int warmup = plugin.getConfig().getInt("teams.home-teleport-warmup-seconds", 3);
        Message.send(player, "&aTeleporting in &f" + warmup + "s&a. Do not move.");
        teamManager.startHomeTeleport(player, optionalTeam.get());
    }

    private void usage(Player player) {
        Message.send(player, "&e/team create <name>, invite <player>, accept, leave, disband, info, sethome, home");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "invite", "accept", "leave", "disband", "info", "sethome", "home").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        return List.of();
    }
}
