package com.friendssmp.command;

import com.friendssmp.gui.BaseGUI;
import com.friendssmp.util.Message;
import com.sun.management.OperatingSystemMXBean;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.List;

public final class PerformanceCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final long startedAtMs;

    public PerformanceCommand(JavaPlugin plugin, long startedAtMs) {
        this.plugin = plugin;
        this.startedAtMs = startedAtMs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&cOnly players can view performance.");
            return true;
        }
        PerformanceGUI gui = new PerformanceGUI(player);
        gui.open(player);
        for (int i = 1; i <= 5; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() == gui) {
                    gui.refresh();
                }
            }, i * 60L);
        }
        return true;
    }

    private final class PerformanceGUI extends BaseGUI {
        private final Player player;

        private PerformanceGUI(Player player) {
            super(27, "Server Performance");
            this.player = player;
        }

        @Override
        protected void render() {
            fill(item(Material.GRAY_STAINED_GLASS_PANE, " "));
            Runtime runtime = Runtime.getRuntime();
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
            long maxMb = runtime.maxMemory() / 1024L / 1024L;
            double cpu = cpuLoad();
            double tps = Bukkit.getTPS()[0];
            Duration uptime = Duration.ofMillis(System.currentTimeMillis() - startedAtMs);
            setItem(10, item(Material.REDSTONE, "§aCPU", List.of("§7Load: §f" + String.format("%.1f%%", cpu * 100.0))));
            setItem(12, item(Material.CLOCK, "§aTPS", List.of("§7Current: §f" + String.format("%.2f", Math.min(20.0, tps)))));
            setItem(14, item(Material.COMPARATOR, "§aMemory", List.of("§7Used: §f" + usedMb + " MB", "§7Max: §f" + maxMb + " MB")));
            setItem(16, item(Material.PLAYER_HEAD, "§aYour Ping", List.of("§7Ping: §f" + player.getPing() + " ms")));
            setItem(22, item(Material.MAP, "§aUptime", List.of("§7" + uptime.toHours() + "h " + (uptime.toMinutes() % 60) + "m")));
        }

        private void refresh() {
            redraw();
        }

        private double cpuLoad() {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof OperatingSystemMXBean osBean) {
                return Math.max(0.0, osBean.getCpuLoad());
            }
            return 0.0;
        }
    }
}
