package com.friendssmp.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class PaginatedGUI<T> extends BaseGUI {
    private final List<T> entries;
    private final int[] contentSlots;
    private int page;

    protected PaginatedGUI(int size, String title, List<T> entries, int[] contentSlots, int page) {
        super(size, title);
        this.entries = entries;
        this.contentSlots = contentSlots;
        this.page = Math.max(0, page);
    }

    @Override
    protected final void render() {
        renderFrame();
        int start = page * contentSlots.length;
        for (int i = 0; i < contentSlots.length && start + i < entries.size(); i++) {
            T entry = entries.get(start + i);
            renderEntry(contentSlots[i], entry);
        }
        if (page > 0) {
            setItem(previousSlot(), navItem("§aPrevious Page"), event -> {
                page--;
                redraw();
            });
        }
        if (start + contentSlots.length < entries.size()) {
            setItem(nextSlot(), navItem("§aNext Page"), event -> {
                page++;
                redraw();
            });
        }
    }

    protected void renderFrame() {
    }

    protected abstract void renderEntry(int slot, T entry);

    protected int previousSlot() {
        return 45;
    }

    protected int nextSlot() {
        return 53;
    }

    protected int page() {
        return page;
    }

    protected List<T> entries() {
        return entries;
    }

    protected ItemStack navItem(String name) {
        return item(Material.ARROW, name);
    }

    protected void ignored(InventoryClickEvent event) {
    }
}
