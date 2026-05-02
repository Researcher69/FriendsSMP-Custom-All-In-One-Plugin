package com.friendssmp;

import com.friendssmp.command.ClaimCommands;
import com.friendssmp.command.DailyCommand;
import com.friendssmp.command.EconomyCommands;
import com.friendssmp.command.StatsCommands;
import com.friendssmp.command.TeamChatCommand;
import com.friendssmp.command.TeamCommand;
import com.friendssmp.gui.ClaimGUIManager;
import com.friendssmp.gui.GUIManager;
import com.friendssmp.gui.ShopGUI;
import com.friendssmp.gui.TeamInfoGUI;
import com.friendssmp.gui.TrustGUIManager;
import com.friendssmp.listener.ClaimListener;
import com.friendssmp.listener.PlayerListener;
import com.friendssmp.listener.TeamListener;
import com.friendssmp.manager.ClaimManager;
import com.friendssmp.manager.ClaimWandManager;
import com.friendssmp.manager.DailyManager;
import com.friendssmp.manager.EconomyManager;
import com.friendssmp.manager.PlayerRegistryManager;
import com.friendssmp.manager.StatsManager;
import com.friendssmp.manager.TeamManager;
import com.friendssmp.storage.Database;
import com.friendssmp.util.Message;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FriendsSMPPlugin extends JavaPlugin {
    private Database database;
    private TeamManager teamManager;
    private ClaimManager claimManager;
    private ClaimWandManager claimWandManager;
    private ClaimGUIManager claimGUIManager;
    private TrustGUIManager trustGUIManager;
    private DailyManager dailyManager;
    private EconomyManager economyManager;
    private PlayerRegistryManager playerRegistryManager;
    private StatsManager statsManager;
    private ShopGUI shopGUI;
    private TeamInfoGUI teamInfoGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Message.init(this);

        database = new Database(this);
        database.init();

        teamManager = new TeamManager(this, database);
        economyManager = new EconomyManager(this, database);
        playerRegistryManager = new PlayerRegistryManager(this, database);
        claimManager = new ClaimManager(this, database);
        claimWandManager = new ClaimWandManager(this, claimManager);
        trustGUIManager = new TrustGUIManager(claimManager, playerRegistryManager);
        claimGUIManager = new ClaimGUIManager(claimManager, trustGUIManager);
        dailyManager = new DailyManager(this, database, economyManager);
        statsManager = new StatsManager(this, database);
        shopGUI = new ShopGUI(this, economyManager);
        teamInfoGUI = new TeamInfoGUI(this, teamManager);

        teamManager.load();
        playerRegistryManager.load();
        for (org.bukkit.OfflinePlayer offlinePlayer : getServer().getOfflinePlayers()) {
            playerRegistryManager.addOffline(offlinePlayer);
        }
        claimManager.load();
        statsManager.loadOnlinePlayers();
        getServer().getOnlinePlayers().forEach(player -> {
            economyManager.loadOnline(player);
            playerRegistryManager.recordJoin(player);
        });

        registerCommands();
        registerListeners();
        getLogger().info("FriendsSMP enabled.");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) {
            statsManager.flushOnlinePlaytime();
        }
        if (database != null) {
            database.shutdown();
        }
        getLogger().info("FriendsSMP disabled.");
    }

    private void registerCommands() {
        TeamCommand teamCommand = new TeamCommand(this, teamManager, teamInfoGUI);
        requireCommand("team").setExecutor(teamCommand);
        requireCommand("team").setTabCompleter(teamCommand);

        TeamChatCommand teamChatCommand = new TeamChatCommand(teamManager);
        requireCommand("teamchat").setExecutor(teamChatCommand);

        ClaimCommands claimCommands = new ClaimCommands(claimManager, claimWandManager, claimGUIManager);
        for (String command : new String[]{"claim", "unclaim", "claims", "trust", "untrust"}) {
            requireCommand(command).setExecutor(claimCommands);
        }

        requireCommand("daily").setExecutor(new DailyCommand(this, dailyManager));
        EconomyCommands economyCommands = new EconomyCommands(this, economyManager);
        requireCommand("balance").setExecutor(economyCommands);
        requireCommand("pay").setExecutor(economyCommands);
        requireCommand("shop").setExecutor(shopGUI);

        StatsCommands statsCommands = new StatsCommands(statsManager);
        requireCommand("stats").setExecutor(statsCommands);
        requireCommand("top").setExecutor(statsCommands);
        requireCommand("top").setTabCompleter(statsCommands);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(statsManager, claimManager, economyManager, playerRegistryManager), this);
        getServer().getPluginManager().registerEvents(new TeamListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new ClaimListener(claimManager), this);
        getServer().getPluginManager().registerEvents(claimWandManager, this);
        getServer().getPluginManager().registerEvents(new GUIManager(), this);
    }

    private PluginCommand requireCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + name);
        }
        return command;
    }
}
