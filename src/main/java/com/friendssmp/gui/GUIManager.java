package com.friendssmp.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GUIManager implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof BaseGUI gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player) || event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
            gui.handleClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof BaseGUI) {
            event.setCancelled(true);
        }
    }
}
