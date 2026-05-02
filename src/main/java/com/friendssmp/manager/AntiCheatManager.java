package com.friendssmp.manager;

import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiCheatManager implements Listener {
    private static final Set<Material> TRACKED_ORES = EnumSet.of(
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.ANCIENT_DEBRIS,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE);
    private static final Set<Material> BASE_STONE = EnumSet.of(
            Material.STONE,
            Material.DEEPSLATE,
            Material.NETHERRACK,
            Material.TUFF,
            Material.GRANITE,
            Material.ANDESITE,
            Material.DIORITE);

    private final JavaPlugin plugin;
    private final EvidenceManager evidenceManager;
    private final JailManager jailManager;
    private final Random random = new Random();
    private final Map<UUID, MiningProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, Decoy> decoys = new ConcurrentHashMap<>();

    public AntiCheatManager(JavaPlugin plugin, EvidenceManager evidenceManager, JailManager jailManager) {
        this.plugin = plugin;
        this.evidenceManager = evidenceManager;
        this.jailManager = jailManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::decaySuspicion, 20L * 60L, 20L * 60L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        MiningProfile profile = profiles.computeIfAbsent(player.getUniqueId(), ignored -> new MiningProfile());

        Decoy decoy = decoys.get(player.getUniqueId());
        if (decoy != null && decoy.location().equals(block.getLocation())) {
            event.setCancelled(true);
            event.setDropItems(false);
            player.sendBlockChange(decoy.location(), decoy.realMaterial().createBlockData());
            decoys.remove(player.getUniqueId());
            addSuspicion(player, block.getLocation(), "Decoy ore targeted", 12);
            return;
        }

        long now = System.currentTimeMillis();
        profile.record(block.getType(), block.getLocation(), now);
        if (TRACKED_ORES.contains(block.getType())) {
            int increase = scoreOreBreak(profile, block.getLocation(), now);
            if (increase > 0) {
                addSuspicion(player, block.getLocation(), "Ore mined: " + block.getType(), increase);
            }
        }
        if (profile.suspicion > 5 && !decoys.containsKey(player.getUniqueId()) && random.nextInt(100) < 12) {
            spawnDecoy(player);
        }
    }

    private int scoreOreBreak(MiningProfile profile, Location location, long now) {
        long oneMinuteAgo = now - 60_000L;
        int ores = profile.countOresSince(oneMinuteAgo);
        int stone = profile.countStoneSince(oneMinuteAgo);
        int score = 0;
        if (ores >= 6) {
            score += 3;
        }
        if (stone >= 8 && ores >= 4 && ores / (double) Math.max(1, stone) > 0.45) {
            score += 4;
        }
        if (profile.lastOreLocation != null && location.distanceSquared(profile.lastOreLocation) > 64 && profile.blocksSinceOre < 12) {
            score += 3;
        }
        profile.lastOreLocation = location.clone();
        profile.blocksSinceOre = 0;
        return score;
    }

    private void addSuspicion(Player player, Location location, String event, int increase) {
        MiningProfile profile = profiles.computeIfAbsent(player.getUniqueId(), ignored -> new MiningProfile());
        profile.suspicion += increase;
        evidenceManager.log(player.getUniqueId(), player.getName(), location, event, increase, profile.suspicion);
        if (profile.suspicion >= 30) {
            broadcastFlag(player, profile.suspicion);
            jailManager.jail(player, "Suspicious mining level " + profile.suspicion);
        } else if ((profile.suspicion >= 20 && !profile.warned20) || (profile.suspicion >= 10 && !profile.warned10)) {
            if (profile.suspicion >= 20) {
                profile.warned20 = true;
            }
            if (profile.suspicion >= 10) {
                profile.warned10 = true;
            }
            broadcastFlag(player, profile.suspicion);
        }
    }

    private void broadcastFlag(Player player, int suspicion) {
        Message.broadcast("&c[AntiCheat] &f" + player.getName() + " &cflagged for suspicious mining (level " + suspicion + ")");
    }

    private void spawnDecoy(Player player) {
        Location base = player.getLocation();
        for (int attempt = 0; attempt < 12; attempt++) {
            Location target = base.clone().add(random.nextInt(9) - 4, random.nextInt(5) - 2, random.nextInt(9) - 4);
            if (!BASE_STONE.contains(target.getBlock().getType())) {
                continue;
            }
            Material fake = target.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER ? Material.ANCIENT_DEBRIS : Material.DIAMOND_ORE;
            decoys.put(player.getUniqueId(), new Decoy(target.getBlock().getLocation(), target.getBlock().getType()));
            player.sendBlockChange(target, fake.createBlockData());
            evidenceManager.log(player.getUniqueId(), player.getName(), target, "Decoy spawned: " + fake, 0, profiles.get(player.getUniqueId()).suspicion);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Decoy decoy = decoys.remove(player.getUniqueId());
                if (decoy != null) {
                    player.sendBlockChange(decoy.location(), decoy.realMaterial().createBlockData());
                }
            }, 20L * 45L);
            return;
        }
    }

    private void decaySuspicion() {
        profiles.values().forEach(profile -> {
            profile.suspicion = Math.max(0, profile.suspicion - 1);
            profile.trim(System.currentTimeMillis() - 120_000L);
        });
    }

    private static final class MiningProfile {
        private final Deque<MinedBlock> mined = new ArrayDeque<>();
        private int suspicion;
        private boolean warned10;
        private boolean warned20;
        private Location lastOreLocation;
        private int blocksSinceOre = 999;

        private void record(Material material, Location location, long time) {
            mined.addLast(new MinedBlock(material, location.clone(), time));
            blocksSinceOre++;
            trim(time - 120_000L);
        }

        private int countOresSince(long time) {
            return (int) mined.stream().filter(block -> block.time() >= time && TRACKED_ORES.contains(block.material())).count();
        }

        private int countStoneSince(long time) {
            return (int) mined.stream().filter(block -> block.time() >= time && BASE_STONE.contains(block.material())).count();
        }

        private void trim(long cutoff) {
            while (!mined.isEmpty() && mined.peekFirst().time() < cutoff) {
                mined.removeFirst();
            }
        }
    }

    private record MinedBlock(Material material, Location location, long time) {
    }

    private record Decoy(Location location, Material realMaterial) {
    }
}
