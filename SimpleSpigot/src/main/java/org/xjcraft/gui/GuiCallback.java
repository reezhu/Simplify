package org.xjcraft.gui;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.Inventory;
import org.xjcraft.bean.IconBundle;

public abstract class GuiCallback {
    @Getter
    @Setter
    String function;

    public abstract void callback(IconBundle iconBundle, int slot, Inventory inventory);
}
