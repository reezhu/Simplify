package org.xjcraft.utils.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

public class BasePlaceholder extends PlaceholderExpansion {
    protected final Plugin plugin;
    private final String keyword;
    private final CompileDBNode node;

    public BasePlaceholder(Plugin plugin, String keyword, CompileDBNode node) {
        this.plugin = plugin;
        this.keyword = keyword;
        this.node = node;
    }

    @Override
    public String onRequest(OfflinePlayer player, String arg) {
        String[] args = arg.split("_", 2);
        String fArg = (args.length >= 2) ? args[1] : "";
        return node.compile(player.getName(), fArg);
    }


    @Override
    public String getIdentifier() {
        return keyword != null ? keyword : plugin.getName();
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

}
