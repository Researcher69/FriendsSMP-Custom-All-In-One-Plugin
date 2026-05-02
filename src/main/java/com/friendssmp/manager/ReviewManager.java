package com.friendssmp.manager;

import com.friendssmp.gui.BaseGUI;
import com.friendssmp.util.Message;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ReviewManager implements Listener {
    private final JavaPlugin plugin;
    private final AdminManager adminManager;
    private final File reviewsFile;
    private final Set<UUID> awaitingReview = ConcurrentHashMap.newKeySet();

    public ReviewManager(JavaPlugin plugin, AdminManager adminManager) {
        this.plugin = plugin;
        this.adminManager = adminManager;
        this.reviewsFile = new File(plugin.getDataFolder(), "reviews.txt");
    }

    public void openReview(Player player) {
        new ReviewGUI().open(player);
    }

    public void openReviews(Player player) {
        if (!adminManager.isAdmin(player)) {
            Message.send(player, "&cYou do not have permission.");
            return;
        }
        List<String> lines = readRecentReviews();
        new ReviewsGUI(lines).open(player);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingReview.remove(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        appendReview(player, message);
        plugin.getServer().getScheduler().runTask(plugin, () -> Message.send(player, "&aReview submitted. Thank you."));
    }

    private void appendReview(Player player, String message) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (FileWriter writer = new FileWriter(reviewsFile, true)) {
                writer.write("[" + Instant.now() + "] " + player.getName() + " (" + player.getUniqueId() + "): " + message);
                writer.write(System.lineSeparator());
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not write review.", exception);
            }
        });
    }

    private List<String> readRecentReviews() {
        if (!reviewsFile.exists()) {
            return List.of("No reviews submitted yet.");
        }
        try {
            List<String> lines = java.nio.file.Files.readAllLines(reviewsFile.toPath());
            int from = Math.max(0, lines.size() - 18);
            return lines.subList(from, lines.size());
        } catch (IOException exception) {
            return List.of("Could not read reviews.txt");
        }
    }

    private final class ReviewGUI extends BaseGUI {
        private ReviewGUI() {
            super(27, "Submit Review");
        }

        @Override
        protected void render() {
            fill(item(Material.GRAY_STAINED_GLASS_PANE, " "));
            setItem(13, item(Material.WRITABLE_BOOK, "§aWrite Review", List.of("§7Click, then type your review in chat.", "§7Your next chat message will be saved.")), event -> {
                Player player = (Player) event.getWhoClicked();
                awaitingReview.add(player.getUniqueId());
                player.closeInventory();
                Message.send(player, "&aType your review in chat. It will not be sent publicly.");
            });
        }
    }

    private final class ReviewsGUI extends BaseGUI {
        private final List<String> lines;

        private ReviewsGUI(List<String> lines) {
            super(54, "Recent Reviews");
            this.lines = lines;
        }

        @Override
        protected void render() {
            fill(item(Material.BLACK_STAINED_GLASS_PANE, " "));
            for (int i = 0; i < Math.min(45, lines.size()); i++) {
                setItem(i, reviewLine(lines.get(i)));
            }
            setItem(49, item(Material.BARRIER, "§cClose"), event -> event.getWhoClicked().closeInventory());
        }

        private ItemStack reviewLine(String line) {
            return item(Material.PAPER, "§fReview", List.of("§7" + line.substring(0, Math.min(80, line.length()))));
        }
    }
}
