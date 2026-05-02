package com.friendssmp.listener;

import com.friendssmp.manager.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class TeamListener implements Listener {
    private final TeamManager teamManager;

    public TeamListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attackerFrom(event.getDamager());
        if (attacker != null && teamManager.isFriendlyDamageBlocked(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    private Player attackerFrom(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
