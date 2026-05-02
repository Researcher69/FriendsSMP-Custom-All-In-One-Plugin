package com.friendssmp.manager;

import com.friendssmp.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClaimWandManager implements Listener {
    private final JavaPlugin plugin;
    private final ClaimManager claimManager;
    private final NamespacedKey wandKey;
    private final Map<UUID, Selection> selections = new HashMap<>();

    public ClaimWandManager(JavaPlugin plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.wandKey = new NamespacedKey(plugin, "claim_wand");
    }

    public void giveWand(Player player) {
        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName("§aClaim Wand");
        meta.setLore(java.util.List.of("§7Left click: position 1", "§7Right click: position 2", "§eSingle use"));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        selections.remove(player.getUniqueId());
        Message.send(player, "&aClaim Wand received. Select two corners to claim land.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null || !isWand(event.getItem())) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation();
        Selection selection = selections.computeIfAbsent(player.getUniqueId(), ignored -> new Selection());
        if (action == Action.LEFT_CLICK_BLOCK) {
            selection.first = location;
            player.sendActionBar(Component.text("Position 1: " + format(location)));
            Message.send(player, "&aPosition 1 set: &f" + format(location));
        } else {
            selection.second = location;
            player.sendActionBar(Component.text("Position 2: " + format(location)));
            Message.send(player, "&aPosition 2 set: &f" + format(location));
        }
        if (selection.first == null || selection.second == null) {
            return;
        }
        ClaimManager.ClaimResult result = claimManager.createRegion(player, selection.first, selection.second);
        switch (result) {
            case SUCCESS -> {
                removeOneWand(player);
                selections.remove(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                Message.send(player, "&aClaim created. Your Claim Wand has been consumed.");
            }
            case DIFFERENT_WORLDS -> Message.send(player, "&cBoth positions must be in the same world.");
            case TOO_LARGE -> Message.send(player, "&cClaims can be at most " + plugin.getConfig().getInt("claims.max-size", 500) + " x " + plugin.getConfig().getInt("claims.max-size", 500) + " blocks.");
            case LIMIT_REACHED -> Message.send(player, "&cYou have reached your claim limit.");
            case OVERLAPS -> Message.send(player, "&cThat region overlaps another claim.");
        }
    }

    private boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) {
            return false;
        }
        Byte value = item.getItemMeta().getPersistentDataContainer().get(wandKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private void removeOneWand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isWand(item)) {
            return;
        }
        item.setAmount(item.getAmount() - 1);
    }

    private String format(Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private static final class Selection {
        private Location first;
        private Location second;
    }
}
