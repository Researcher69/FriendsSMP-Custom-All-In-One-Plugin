package com.friendssmp.manager;

import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class JailManager implements Listener {
    private static final List<String> BLOCKED_COMMANDS = List.of("/tpa", "/tpahere", "/home", "/back", "/spawn");

    private final JavaPlugin plugin;
    private final AdminManager adminManager;
    private final File dataFile;
    private final File inventoryFolder;
    private final Map<UUID, JailRecord> jailed = new HashMap<>();
    private Location jailLocation;

    public JailManager(JavaPlugin plugin, AdminManager adminManager) {
        this.plugin = plugin;
        this.adminManager = adminManager;
        this.dataFile = new File(plugin.getDataFolder(), "jailed.yml");
        this.inventoryFolder = new File(plugin.getDataFolder(), "jailed-inventories");
        if (!inventoryFolder.exists() && !inventoryFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create jailed inventory folder.");
        }
        load();
    }

    public void setupJail() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("jail.world", ""));
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
            Location spawn = world.getSpawnLocation().clone().add(8, 1, 8);
            plugin.getConfig().set("jail.world", world.getName());
            plugin.getConfig().set("jail.x", spawn.getBlockX());
            plugin.getConfig().set("jail.y", spawn.getBlockY());
            plugin.getConfig().set("jail.z", spawn.getBlockZ());
            plugin.saveConfig();
        }
        jailLocation = new Location(world,
                plugin.getConfig().getDouble("jail.x"),
                plugin.getConfig().getDouble("jail.y"),
                plugin.getConfig().getDouble("jail.z"),
                0.0f,
                0.0f);
        buildCage(jailLocation);
    }

    public boolean isJailed(UUID uuid) {
        return jailed.containsKey(uuid);
    }

    public void jail(Player player, String reason) {
        if (isJailed(player.getUniqueId())) {
            teleportAndDebuff(player);
            return;
        }
        ItemStack evidenceShulker = evidenceShulker(player);
        jailed.put(player.getUniqueId(), JailRecord.from(player.getLocation()));
        saveInventory(player);
        player.getInventory().clear();
        save();
        teleportAndDebuff(player);
        Message.broadcast("&c[AntiCheat] &f" + player.getName() + "&c was jailed: &f" + reason);
        giveEvidenceShulker(evidenceShulker);
    }

    public boolean unjail(Player player) {
        JailRecord record = jailed.remove(player.getUniqueId());
        if (record == null) {
            return false;
        }
        restoreInventory(player);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        Location returnLocation = record.toLocation();
        if (returnLocation != null) {
            player.teleport(returnLocation);
        }
        save();
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (isJailed(event.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> teleportAndDebuff(event.getPlayer()), 20L);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isJailed(event.getPlayer().getUniqueId())) {
            return;
        }
        String message = event.getMessage().toLowerCase();
        for (String blocked : BLOCKED_COMMANDS) {
            if (message.equals(blocked) || message.startsWith(blocked + " ")) {
                event.setCancelled(true);
                Message.send(event.getPlayer(), "&cYou cannot use teleport commands while jailed.");
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isJailed(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            Message.send(event.getPlayer(), "&cYou cannot break blocks while jailed.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (isJailed(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            Message.send(event.getPlayer(), "&cYou cannot place blocks while jailed.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!isJailed(event.getPlayer().getUniqueId()) || event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }
        if (!event.getTo().getWorld().equals(jailLocation.getWorld()) || event.getTo().distanceSquared(jailLocation) > 25.0) {
            event.setCancelled(true);
            teleportAndDebuff(event.getPlayer());
        }
    }

    private void teleportAndDebuff(Player player) {
        player.teleport(jailLocation.clone().add(0.5, 1.0, 0.5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, Integer.MAX_VALUE, 2, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 1, true, false));
    }

    private void buildCage(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -2; z <= 2; z++) {
                    boolean floor = y == 0;
                    boolean roof = y == 4;
                    boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2;
                    if (!floor && !roof && !wall) {
                        continue;
                    }
                    Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                    block.setType(floor || roof ? Material.OBSIDIAN : Material.IRON_BARS, false);
                }
            }
        }
        world.getBlockAt(cx, cy, cz).setType(Material.WATER, false);
    }

    private void saveInventory(Player player) {
        File file = inventoryFile(player.getUniqueId());
        try (BukkitObjectOutputStream output = new BukkitObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(player.getInventory().getContents());
            output.writeObject(player.getInventory().getArmorContents());
            output.writeObject(player.getInventory().getItemInOffHand());
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save jailed inventory for " + player.getName(), exception);
        }
    }

    private void restoreInventory(Player player) {
        File file = inventoryFile(player.getUniqueId());
        if (!file.exists()) {
            return;
        }
        try (BukkitObjectInputStream input = new BukkitObjectInputStream(new FileInputStream(file))) {
            player.getInventory().setContents((ItemStack[]) input.readObject());
            player.getInventory().setArmorContents((ItemStack[]) input.readObject());
            player.getInventory().setItemInOffHand((ItemStack) input.readObject());
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not restore jailed inventory for " + player.getName(), exception);
        }
        if (!file.delete()) {
            plugin.getLogger().warning("Could not delete jailed inventory file for " + player.getName());
        }
    }

    private ItemStack evidenceShulker(Player jailedPlayer) {
        ItemStack shulker = new ItemStack(Material.RED_SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) shulker.getItemMeta();
        meta.setDisplayName("§cJailed Inventory: " + jailedPlayer.getName());
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        ItemStack[] contents = jailedPlayer.getInventory().getContents();
        for (int i = 0; i < Math.min(27, contents.length); i++) {
            box.getInventory().setItem(i, contents[i]);
        }
        meta.setBlockState(box);
        shulker.setItemMeta(meta);
        return shulker;
    }

    private void giveEvidenceShulker(ItemStack shulker) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (adminManager.isAdmin(online)) {
                online.getInventory().addItem(shulker.clone()).values().forEach(item -> online.getWorld().dropItemNaturally(online.getLocation(), item));
                return;
            }
        }
    }

    private void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : config.getKeys(false)) {
            jailed.put(UUID.fromString(key), new JailRecord(
                    config.getString(key + ".world"),
                    config.getDouble(key + ".x"),
                    config.getDouble(key + ".y"),
                    config.getDouble(key + ".z"),
                    (float) config.getDouble(key + ".yaw"),
                    (float) config.getDouble(key + ".pitch")));
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        jailed.forEach((uuid, record) -> {
            config.set(uuid + ".world", record.world());
            config.set(uuid + ".x", record.x());
            config.set(uuid + ".y", record.y());
            config.set(uuid + ".z", record.z());
            config.set(uuid + ".yaw", record.yaw());
            config.set(uuid + ".pitch", record.pitch());
        });
        try {
            config.save(dataFile);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save jailed players.", exception);
        }
    }

    private File inventoryFile(UUID uuid) {
        return new File(inventoryFolder, uuid + ".dat");
    }

    private record JailRecord(String world, double x, double y, double z, float yaw, float pitch) {
        private static JailRecord from(Location location) {
            return new JailRecord(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        }

        private Location toLocation() {
            World bukkitWorld = Bukkit.getWorld(world);
            return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z, yaw, pitch);
        }
    }
}
