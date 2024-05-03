package org.xjcraft.utils.placeholder;

import org.bukkit.plugin.Plugin;

/**
 * 用于兼容pe的placeholder注册接口
 */
public interface CompileDB {
    static void register(Plugin plugin, CompileDBNode node) {
        register(plugin, null, node);
    }

    static void register(Plugin plugin, String keyword, CompileDBNode node) {
        boolean register = new BasePlaceholder(plugin, keyword, node).register();
        if (!register) {
            plugin.getLogger().warning("placeHolder注册失败" + (keyword == null ? plugin.getName() : keyword));
        }
    }
}
