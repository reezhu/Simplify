package org.xjcraft.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.xjcraft.bean.IconBundle;

public abstract class GuiListener implements Listener {
    GuiHolder guiHolder;
    Player player;

    public GuiListener(GuiHolder guiHolder, Player player) {
        this.guiHolder = guiHolder;
        this.player = player;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != guiHolder) return;
        if (event.getWhoClicked() != player) return;
        event.setCancelled(true);
        if (guiHolder.getIcons().containsKey(event.getRawSlot()))
            click(guiHolder.getIcons().get(event.getRawSlot()), event.getRawSlot(), event.getInventory());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != guiHolder) return;
        if (event.getPlayer() != player) return;
        close();
    }

    protected abstract void click(IconBundle iconBundle, int slot, Inventory inventory);

    protected abstract void close();

}
