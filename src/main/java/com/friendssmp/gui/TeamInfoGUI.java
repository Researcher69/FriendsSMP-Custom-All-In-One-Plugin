package com.friendssmp.gui;

import com.friendssmp.manager.TeamManager;
import com.friendssmp.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TeamInfoGUI {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;

    public TeamInfoGUI(JavaPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public void open(Player player, Team team) {
        new TeamGUI(team).open(player);
    }

    private final class TeamGUI extends BaseGUI {
        private final Team team;

        private TeamGUI(Team team) {
            super(27, "Team: " + team.name());
            this.team = team;
        }

        @Override
        protected void render() {
            fill(item(Material.GRAY_STAINED_GLASS_PANE, " "));
            setItem(4, item(Material.SHIELD, "§a" + team.name(), List.of(
                    "§7Members: §f" + team.members().size() + "/" + plugin.getConfig().getInt("teams.max-size", 4),
                    "§7Friendly PvP: §f" + (plugin.getConfig().getBoolean("teams.friendly-pvp-disabled", true) ? "Disabled" : "Enabled"),
                    "§7Roster: §f" + String.join(", ", teamManager.memberNames(team)))));
            Location home = team.home();
            setItem(22, item(Material.RED_BED, "§aTeam Home", home == null
                    ? List.of("§cNot set")
                    : List.of("§7World: §f" + home.getWorld().getName(), "§7Coords: §f" + home.getBlockX() + ", " + home.getBlockY() + ", " + home.getBlockZ())));
            int slot = 10;
            for (UUID uuid : team.members()) {
                if (slot > 16) {
                    break;
                }
                setItem(slot++, head(uuid, team.ownerUuid().equals(uuid)));
            }
        }

        private ItemStack head(UUID uuid, boolean owner) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwningPlayer(offlinePlayer);
            Player online = Bukkit.getPlayer(uuid);
            meta.setDisplayName((online == null ? "§7" : "§a") + (offlinePlayer.getName() == null ? uuid.toString().substring(0, 8) : offlinePlayer.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(owner ? "§7Role: §eOwner" : "§7Role: §fMember");
            lore.add(online == null ? "§7Status: §cOffline" : "§7Status: §aOnline");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
    }
}
