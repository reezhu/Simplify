package org.xjcraft.gui;

import lombok.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.xjcraft.bean.IconBundle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class GuiHolder implements InventoryHolder {
    Inventory inventory;
    Map<Integer, IconBundle> icons;
    Plugin plugin;
    int size;
    String title;
    @Getter
    Map<String, GuiCallback> functions;


    public GuiHolder(Plugin plugin, List<IconBundle> iconBundles, int size, String title) {
        this.plugin = plugin;
        this.size = size;
        this.title = title;
        icons = new HashMap<>();
        functions = new HashMap<>();
        inventory = Bukkit.createInventory(this, size, title);
        for (IconBundle iconBundle : iconBundles) {
            if (iconBundle == null) return;
            Integer slot = iconBundle.getSlot();
            if (slot < 0 && slot >= size) return;
            icons.put(slot, iconBundle);
            inventory.setItem(slot, iconBundle.getHover());

        }

    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void openGui(Player player) {
        player.openInventory(inventory);
        GuiListener guiListener = new GuiListener(this, player) {
            @Override
            protected void click(IconBundle iconBundle, int slot, Inventory inventory) {
                if (functions.containsKey(iconBundle.getFunction()))
                    functions.get(iconBundle.getFunction()).callback(iconBundle, slot, inventory);
            }

            @Override
            protected void close() {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    HandlerList.unregisterAll(this);
                });

            }
        };
        Bukkit.getPluginManager().registerEvents(guiListener, plugin);
    }


}
