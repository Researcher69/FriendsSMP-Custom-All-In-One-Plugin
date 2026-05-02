package com.friendssmp.listener;

import com.friendssmp.manager.ClaimManager;
import com.friendssmp.util.Message;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.EnumSet;
import java.util.Set;

public final class ClaimListener implements Listener {
    private static final Set<Material> CONTAINER_MATERIALS = EnumSet.of(
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.BARREL,
            Material.SHULKER_BOX,
            Material.WHITE_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX);

    private final ClaimManager claimManager;

    public ClaimListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!claimManager.canInteract(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            Message.send(event.getPlayer(), "&cYou cannot break blocks in this claim.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!claimManager.canInteract(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            Message.send(event.getPlayer(), "&cYou cannot place blocks in this claim.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Location location = event.getInventory().getLocation();
        if (location == null) {
            return;
        }
        Block block = location.getBlock();
        if (!CONTAINER_MATERIALS.contains(block.getType())) {
            return;
        }
        if (!claimManager.canInteract(player, block.getLocation())) {
            event.setCancelled(true);
            Message.send(player, "&cYou cannot open containers in this claim.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null) {
            return;
        }
        claimManager.at(event.getEntity().getLocation()).ifPresent(claim -> {
            if (!claim.entityDamageEnabled() && !claim.canBuild(attacker.getUniqueId()) && !attacker.hasPermission("friendssmp.admin")) {
                event.setCancelled(true);
                Message.send(attacker, "&cEntity damage is disabled in this claim.");
            }
        });
    }

    private Player attackingPlayer(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
