package com.friendssmp.manager;

import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TPAManager implements CommandExecutor {
    private final JavaPlugin plugin;
    private final JailManager jailManager;
    private final Map<UUID, TpaRequest> requestsByTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public TPAManager(JavaPlugin plugin, JailManager jailManager) {
        this.plugin = plugin;
        this.jailManager = jailManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::expireRequests, 20L * 10L, 20L * 10L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&cOnly players can use TPA.");
            return true;
        }
        if (jailManager.isJailed(player.getUniqueId())) {
            Message.send(player, "&cYou cannot use TPA while jailed.");
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "tpa" -> request(player, args, false);
            case "tpahere" -> request(player, args, true);
            case "tpaccept" -> accept(player);
            case "tpdeny" -> deny(player);
            default -> Message.send(player, "&cUnknown TPA command.");
        }
        return true;
    }

    private void request(Player sender, String[] args, boolean here) {
        if (args.length != 1) {
            Message.send(sender, here ? "&cUsage: /tpahere <player>" : "&cUsage: /tpa <player>");
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong("tpa.cooldown-seconds", 30) * 1000L;
        if (cooldowns.getOrDefault(sender.getUniqueId(), 0L) > now) {
            Message.send(sender, "&cWait before sending another teleport request.");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.getUniqueId().equals(sender.getUniqueId())) {
            Message.send(sender, "&cThat player is not available.");
            return;
        }
        if (jailManager.isJailed(target.getUniqueId())) {
            Message.send(sender, "&cThat player is jailed.");
            return;
        }
        long expiresAt = now + plugin.getConfig().getLong("tpa.expire-seconds", 60) * 1000L;
        requestsByTarget.put(target.getUniqueId(), new TpaRequest(sender.getUniqueId(), target.getUniqueId(), here, expiresAt));
        cooldowns.put(sender.getUniqueId(), now + cooldown);
        Message.send(sender, "&aTeleport request sent to &f" + target.getName() + "&a.");
        Message.send(target, here
                ? "&e" + sender.getName() + " wants you to teleport to them. Use /tpaccept or /tpdeny."
                : "&e" + sender.getName() + " wants to teleport to you. Use /tpaccept or /tpdeny.");
        target.playSound(target.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void accept(Player target) {
        TpaRequest request = requestsByTarget.remove(target.getUniqueId());
        if (request == null || request.expiresAt() < System.currentTimeMillis()) {
            Message.send(target, "&cNo active teleport request.");
            return;
        }
        Player sender = Bukkit.getPlayer(request.sender());
        if (sender == null || jailManager.isJailed(sender.getUniqueId())) {
            Message.send(target, "&cThat teleport request is no longer valid.");
            return;
        }
        if (request.here()) {
            target.teleport(sender.getLocation());
        } else {
            sender.teleport(target.getLocation());
        }
        Message.send(target, "&aTeleport request accepted.");
        Message.send(sender, "&aTeleport request accepted.");
    }

    private void deny(Player target) {
        TpaRequest request = requestsByTarget.remove(target.getUniqueId());
        if (request == null) {
            Message.send(target, "&cNo active teleport request.");
            return;
        }
        Player sender = Bukkit.getPlayer(request.sender());
        if (sender != null) {
            Message.send(sender, "&cTeleport request denied.");
        }
        Message.send(target, "&aTeleport request denied.");
    }

    private void expireRequests() {
        long now = System.currentTimeMillis();
        requestsByTarget.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
    }

    private record TpaRequest(UUID sender, UUID target, boolean here, long expiresAt) {
    }
}
