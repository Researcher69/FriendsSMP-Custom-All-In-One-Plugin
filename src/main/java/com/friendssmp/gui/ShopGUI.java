package com.friendssmp.gui;

import com.friendssmp.manager.EconomyManager;
import com.friendssmp.util.Message;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ShopGUI implements CommandExecutor {
    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private final File shopFile;
    private List<ShopCategory> categories = List.of();

    public ShopGUI(JavaPlugin plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        reload();
    }

    public void reload() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
        List<ShopCategory> loadedCategories = new ArrayList<>();
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) {
            categories = List.of();
            return;
        }
        for (String categoryKey : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryKey);
            if (categorySection == null) {
                continue;
            }
            Material icon = material(categorySection.getString("icon"), Material.CHEST);
            int slot = categorySection.getInt("slot", 13);
            String name = categorySection.getString("name", displayName(categoryKey));
            List<ShopItem> items = new ArrayList<>();
            ConfigurationSection itemsSection = categorySection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection == null) {
                        continue;
                    }
                    Material material = material(itemSection.getString("material"), Material.STONE);
                    int price = Math.max(1, itemSection.getInt("price", 1));
                    String itemName = itemSection.getString("name", displayName(itemKey));
                    items.add(new ShopItem(material, itemName, price));
                }
            }
            loadedCategories.add(new ShopCategory(categoryKey, name, icon, slot, List.copyOf(items)));
        }
        loadedCategories.sort(Comparator.comparingInt(ShopCategory::slot));
        categories = List.copyOf(loadedCategories);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("friendssmp.admin")) {
                Message.send(sender, "&cYou do not have permission to reload the shop.");
                return true;
            }
            reload();
            Message.send(sender, "&aShop configuration reloaded.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&cOnly players can open the shop.");
            return true;
        }
        new MainShopGUI(player).open(player);
        return true;
    }

    private Material material(String name, Material fallback) {
        if (name == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(name);
        return material == null ? fallback : material;
    }

    private String displayName(String key) {
        String[] parts = key.toLowerCase(Locale.ROOT).split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
            }
        }
        return String.join(" ", words);
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private final class MainShopGUI extends BaseGUI {
        private MainShopGUI(Player player) {
            super(27, "SMP Shop");
        }

        @Override
        protected void render() {
            fill(item(Material.GRAY_STAINED_GLASS_PANE, " "));
            setItem(4, item(Material.EMERALD, "§aCoin Shop", List.of("§7Choose a category.")));
            for (ShopCategory category : categories) {
                setItem(category.slot(), icon(category.icon(), "§a" + category.name(), List.of("§7Items: §f" + category.items().size(), "§eClick to browse")),
                        event -> {
                            Player player = (Player) event.getWhoClicked();
                            new CategoryShopGUI(category).open(player);
                        });
            }
        }
    }

    private final class CategoryShopGUI extends PaginatedGUI<ShopItem> {
        private final ShopCategory category;

        private CategoryShopGUI(ShopCategory category) {
            super(54, "Shop: " + category.name(), category.items(), contentSlots(), 0);
            this.category = category;
        }

        @Override
        protected void renderFrame() {
            fill(item(Material.BLACK_STAINED_GLASS_PANE, " "));
            setItem(4, icon(category.icon(), "§a" + category.name(), List.of("§7Left click: buy 1", "§7Shift left: buy stack")));
            setItem(49, item(Material.BARRIER, "§cBack"), event -> new MainShopGUI((Player) event.getWhoClicked()).open((Player) event.getWhoClicked()));
        }

        @Override
        protected void renderEntry(int slot, ShopItem shopItem) {
            setItem(slot, icon(shopItem.material(), "§f" + shopItem.name(), List.of(
                    "§7Price: §e" + shopItem.price() + " coins",
                    "§eLeft click: buy 1",
                    "§eShift left: buy stack")), event -> {
                if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.SHIFT_LEFT) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int amount = event.getClick() == ClickType.SHIFT_LEFT ? shopItem.material().getMaxStackSize() : 1;
                int cost = shopItem.price() * amount;
                economyManager.withdraw(player, cost).thenAccept(success -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (!success) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                        Message.send(player, "&cNot enough coins.");
                        return;
                    }
                    player.getInventory().addItem(new ItemStack(shopItem.material(), amount))
                            .values()
                            .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
                    Message.send(player, "&aPurchased &f" + amount + "x " + shopItem.name() + "&a for &f" + cost + " coins&a.");
                }));
            });
        }
    }

    private static int[] contentSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    private record ShopCategory(String key, String name, Material icon, int slot, List<ShopItem> items) {
    }

    private record ShopItem(Material material, String name, int price) {
    }
}
