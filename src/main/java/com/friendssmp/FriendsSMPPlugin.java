package com.friendssmp;

import com.friendssmp.command.ClaimCommands;
import com.friendssmp.command.AdminCommands;
import com.friendssmp.command.DailyCommand;
import com.friendssmp.command.EconomyCommands;
import com.friendssmp.command.JailCommands;
import com.friendssmp.command.PerformanceCommand;
import com.friendssmp.command.ReviewCommands;
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
import com.friendssmp.manager.AdminManager;
import com.friendssmp.manager.AntiCheatManager;
import com.friendssmp.manager.DailyManager;
import com.friendssmp.manager.EconomyManager;
import com.friendssmp.manager.EvidenceManager;
import com.friendssmp.manager.JailManager;
import com.friendssmp.manager.PlayerRegistryManager;
import com.friendssmp.manager.ReviewManager;
import com.friendssmp.manager.StatsManager;
import com.friendssmp.manager.TPAManager;
import com.friendssmp.manager.TeamManager;
import com.friendssmp.storage.Database;
import com.friendssmp.util.Message;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FriendsSMPPlugin extends JavaPlugin {
    private Database database;
    private long startedAtMs;
    private AdminManager adminManager;
    private EvidenceManager evidenceManager;
    private JailManager jailManager;
    private AntiCheatManager antiCheatManager;
    private TPAManager tpaManager;
    private ReviewManager reviewManager;
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
        startedAtMs = System.currentTimeMillis();
        Message.init(this);

        database = new Database(this);
        database.init();

        adminManager = new AdminManager(this);
        evidenceManager = new EvidenceManager(this);
        teamManager = new TeamManager(this, database);
        economyManager = new EconomyManager(this, database);
        playerRegistryManager = new PlayerRegistryManager(this, database);
        claimManager = new ClaimManager(this, database);
        claimWandManager = new ClaimWandManager(this, claimManager);
        trustGUIManager = new TrustGUIManager(claimManager, playerRegistryManager);
        claimGUIManager = new ClaimGUIManager(claimManager, trustGUIManager);
        dailyManager = new DailyManager(this, database, economyManager);
        statsManager = new StatsManager(this, database);
        jailManager = new JailManager(this, adminManager);
        jailManager.setupJail();
        antiCheatManager = new AntiCheatManager(this, evidenceManager, jailManager);
        tpaManager = new TPAManager(this, jailManager);
        reviewManager = new ReviewManager(this, adminManager);
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
        for (String command : new String[]{"tpa", "tpahere", "tpaccept", "tpdeny"}) {
            requireCommand(command).setExecutor(tpaManager);
        }
        JailCommands jailCommands = new JailCommands(adminManager, jailManager);
        requireCommand("jail").setExecutor(jailCommands);
        requireCommand("unjail").setExecutor(jailCommands);
        AdminCommands adminCommands = new AdminCommands(adminManager);
        requireCommand("setadmin").setExecutor(adminCommands);
        requireCommand("removeadmin").setExecutor(adminCommands);
        ReviewCommands reviewCommands = new ReviewCommands(reviewManager);
        requireCommand("review").setExecutor(reviewCommands);
        requireCommand("reviews").setExecutor(reviewCommands);
        requireCommand("appeal").setExecutor(reviewCommands);
        requireCommand("performance").setExecutor(new PerformanceCommand(this, startedAtMs));

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
        getServer().getPluginManager().registerEvents(jailManager, this);
        getServer().getPluginManager().registerEvents(antiCheatManager, this);
        getServer().getPluginManager().registerEvents(reviewManager, this);
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
