package com.friendssmp.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseGUI implements InventoryHolder {
    private final int size;
    private final String title;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();
    private Inventory inventory;

    protected BaseGUI(int size, String title) {
        this.size = size;
        this.title = title;
    }

    public final void open(Player player) {
        inventory = Bukkit.createInventory(this, size, title);
        redraw();
        player.openInventory(inventory);
    }

    protected final void redraw() {
        clickHandlers.clear();
        inventory.clear();
        render();
    }

    protected abstract void render();

    public final void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> handler = clickHandlers.get(event.getSlot());
        if (handler != null) {
            handler.accept(event);
        }
    }

    protected final void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected final void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        clickHandlers.put(slot, handler);
    }

    protected final void fill(ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }

    protected final ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    protected final ItemStack item(Material material, String name) {
        return item(material, name, List.of());
    }

    protected final Inventory inventory() {
        return inventory;
    }

    @Override
    public final Inventory getInventory() {
        return inventory;
    }
}
