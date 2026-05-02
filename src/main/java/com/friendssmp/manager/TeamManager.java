package com.friendssmp.manager;

import com.friendssmp.model.Team;
import com.friendssmp.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class TeamManager {
    private final JavaPlugin plugin;
    private final Database database;
    private final Map<Integer, Team> teamsById = new ConcurrentHashMap<>();
    private final Map<UUID, Team> teamsByMember = new ConcurrentHashMap<>();
    private final Map<String, Integer> idsByLowerName = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> invites = new ConcurrentHashMap<>();
    private final Map<UUID, Long> homeCooldowns = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public TeamManager(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void load() {
        try (var connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM teams")) {
            int maxId = 0;
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                Team team = new Team(id, resultSet.getString("name"), UUID.fromString(resultSet.getString("owner_uuid")));
                team.home(readHome(resultSet));
                teamsById.put(id, team);
                idsByLowerName.put(team.name().toLowerCase(Locale.ROOT), id);
                maxId = Math.max(maxId, id);
            }
            nextId.set(maxId + 1);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load teams.", exception);
        }

        try (var connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM team_members")) {
            while (resultSet.next()) {
                Team team = teamsById.get(resultSet.getInt("team_id"));
                if (team == null) {
                    continue;
                }
                UUID uuid = UUID.fromString(resultSet.getString("player_uuid"));
                team.members().add(uuid);
                teamsByMember.put(uuid, team);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load team members.", exception);
        }
    }

    public Optional<Team> teamOf(UUID playerUuid) {
        return Optional.ofNullable(teamsByMember.get(playerUuid));
    }

    public Optional<Team> byName(String name) {
        Integer id = idsByLowerName.get(name.toLowerCase(Locale.ROOT));
        return id == null ? Optional.empty() : Optional.ofNullable(teamsById.get(id));
    }

    public boolean createTeam(Player owner, String name) {
        if (!name.matches("[A-Za-z0-9_]{3,16}") || teamOf(owner.getUniqueId()).isPresent() || byName(name).isPresent()) {
            return false;
        }
        int id = nextId.getAndIncrement();
        Team team = new Team(id, name, owner.getUniqueId());
        teamsById.put(id, team);
        teamsByMember.put(owner.getUniqueId(), team);
        idsByLowerName.put(name.toLowerCase(Locale.ROOT), id);
        database.executeAsync(connection -> {
            try (PreparedStatement teamStatement = connection.prepareStatement("INSERT INTO teams(id, name, owner_uuid) VALUES(?, ?, ?)");
                 PreparedStatement memberStatement = connection.prepareStatement("INSERT INTO team_members(team_id, player_uuid, player_name) VALUES(?, ?, ?)")) {
                teamStatement.setInt(1, id);
                teamStatement.setString(2, name);
                teamStatement.setString(3, owner.getUniqueId().toString());
                teamStatement.executeUpdate();
                memberStatement.setInt(1, id);
                memberStatement.setString(2, owner.getUniqueId().toString());
                memberStatement.setString(3, owner.getName());
                memberStatement.executeUpdate();
            }
        });
        return true;
    }

    public boolean invite(Player inviter, Player target) {
        Optional<Team> optionalTeam = teamOf(inviter.getUniqueId());
        if (optionalTeam.isEmpty() || teamOf(target.getUniqueId()).isPresent()) {
            return false;
        }
        Team team = optionalTeam.get();
        if (!team.ownerUuid().equals(inviter.getUniqueId()) || team.members().size() >= plugin.getConfig().getInt("teams.max-size", 4)) {
            return false;
        }
        invites.put(target.getUniqueId(), team.id());
        return true;
    }

    public Optional<Team> accept(Player player) {
        Integer teamId = invites.remove(player.getUniqueId());
        Team team = teamId == null ? null : teamsById.get(teamId);
        if (team == null || team.members().size() >= plugin.getConfig().getInt("teams.max-size", 4) || teamOf(player.getUniqueId()).isPresent()) {
            return Optional.empty();
        }
        addMember(team, player.getUniqueId(), player.getName());
        return Optional.of(team);
    }

    public boolean leave(Player player) {
        Team team = teamsByMember.get(player.getUniqueId());
        if (team == null || team.ownerUuid().equals(player.getUniqueId())) {
            return false;
        }
        removeMember(team, player.getUniqueId());
        return true;
    }

    public boolean disband(Player player) {
        Team team = teamsByMember.get(player.getUniqueId());
        if (team == null || !team.ownerUuid().equals(player.getUniqueId())) {
            return false;
        }
        teamsById.remove(team.id());
        idsByLowerName.remove(team.name().toLowerCase(Locale.ROOT));
        for (UUID member : new ArrayList<>(team.members())) {
            teamsByMember.remove(member);
        }
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM teams WHERE id = ?")) {
                statement.setInt(1, team.id());
                statement.executeUpdate();
            }
        });
        return true;
    }

    public void setHome(Team team, Location location) {
        team.home(location);
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE teams SET home_world = ?, home_x = ?, home_y = ?, home_z = ?, home_yaw = ?, home_pitch = ? WHERE id = ?
                    """)) {
                statement.setString(1, location.getWorld().getName());
                statement.setDouble(2, location.getX());
                statement.setDouble(3, location.getY());
                statement.setDouble(4, location.getZ());
                statement.setFloat(5, location.getYaw());
                statement.setFloat(6, location.getPitch());
                statement.setInt(7, team.id());
                statement.executeUpdate();
            }
        });
    }

    public boolean isFriendlyDamageBlocked(Player attacker, Player victim) {
        if (!plugin.getConfig().getBoolean("teams.friendly-pvp-disabled", true)) {
            return false;
        }
        Team team = teamsByMember.get(attacker.getUniqueId());
        return team != null && team.members().contains(victim.getUniqueId());
    }

    public boolean isHomeOnCooldown(Player player) {
        long expiry = homeCooldowns.getOrDefault(player.getUniqueId(), 0L);
        return expiry > System.currentTimeMillis();
    }

    public long cooldownSecondsLeft(Player player) {
        return Math.max(0, (homeCooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis() + 999) / 1000);
    }

    public void startHomeTeleport(Player player, Team team) {
        Location home = team.home();
        if (home == null) {
            return;
        }
        int cooldown = plugin.getConfig().getInt("teams.home-teleport-cooldown-seconds", 60);
        int warmup = plugin.getConfig().getInt("teams.home-teleport-warmup-seconds", 3);
        Location start = player.getLocation().clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || start.distanceSquared(player.getLocation()) > 1.0) {
                player.sendMessage("Teleport cancelled because you moved.");
                return;
            }
            player.teleport(home);
            homeCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldown * 1000L);
        }, warmup * 20L);
    }

    public void sendTeamChat(Player sender, String message) {
        Team team = teamsByMember.get(sender.getUniqueId());
        if (team == null) {
            sender.sendMessage("You are not in a team.");
            return;
        }
        String formatted = "§2[Team] §f" + sender.getName() + ": §a" + message;
        for (UUID uuid : team.members()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(formatted);
            }
        }
    }

    public Collection<String> memberNames(Team team) {
        List<String> names = team.members().stream()
                .map(uuid -> Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString().substring(0, 8)))
                .sorted(Comparator.naturalOrder())
                .toList();
        return names;
    }

    private void addMember(Team team, UUID uuid, String name) {
        team.members().add(uuid);
        teamsByMember.put(uuid, team);
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO team_members(team_id, player_uuid, player_name) VALUES(?, ?, ?)")) {
                statement.setInt(1, team.id());
                statement.setString(2, uuid.toString());
                statement.setString(3, name);
                statement.executeUpdate();
            }
        });
    }

    private void removeMember(Team team, UUID uuid) {
        team.members().remove(uuid);
        teamsByMember.remove(uuid);
        database.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM team_members WHERE player_uuid = ?")) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            }
        });
    }

    private Location readHome(ResultSet resultSet) throws Exception {
        String worldName = resultSet.getString("home_world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, resultSet.getDouble("home_x"), resultSet.getDouble("home_y"), resultSet.getDouble("home_z"),
                resultSet.getFloat("home_yaw"), resultSet.getFloat("home_pitch"));
    }
}
