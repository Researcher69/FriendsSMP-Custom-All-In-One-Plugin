package com.friendssmp.listener;

import com.friendssmp.manager.ClaimManager;
import com.friendssmp.manager.EconomyManager;
import com.friendssmp.manager.PlayerRegistryManager;
import com.friendssmp.manager.StatsManager;
import com.friendssmp.model.Claim;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerListener implements Listener {
    private final StatsManager statsManager;
    private final ClaimManager claimManager;
    private final EconomyManager economyManager;
    private final PlayerRegistryManager playerRegistryManager;
    private final Map<UUID, String> lastClaimOwner = new ConcurrentHashMap<>();

    public PlayerListener(StatsManager statsManager, ClaimManager claimManager, EconomyManager economyManager, PlayerRegistryManager playerRegistryManager) {
        this.statsManager = statsManager;
        this.claimManager = claimManager;
        this.economyManager = economyManager;
        this.playerRegistryManager = playerRegistryManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        statsManager.handleJoin(event.getPlayer());
        economyManager.loadOnline(event.getPlayer());
        playerRegistryManager.recordJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        statsManager.handleQuit(event.getPlayer());
        lastClaimOwner.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        statsManager.addDeath(victim);
        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            statsManager.addKill(killer);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) {
            return;
        }
        Player player = event.getPlayer();
        Optional<Claim> claim = claimManager.at(event.getTo());
        String owner = claim.map(Claim::ownerName).orElse("");
        String previous = lastClaimOwner.put(player.getUniqueId(), owner);
        if (claim.isPresent() && !owner.equals(previous)) {
            player.sendActionBar(Component.text("Entering " + owner + "'s land"));
        }
    }

    private boolean sameBlock(org.bukkit.Location first, org.bukkit.Location second) {
        return first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
