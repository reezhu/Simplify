package org.xjcraft.example;

import org.bukkit.plugin.java.JavaPlugin;
import org.xjcraft.CommonPlugin;
import org.xjcraft.api.CommonCommandExecutor;

public final class SimpleSpigotExample extends CommonPlugin {

    @Override
    public void onEnable() {
        loadConfigs();
        registerCommand(new CommandExample(this));

    }

    @Override
    public void onDisable() {
        saveConfig();
    }
}
