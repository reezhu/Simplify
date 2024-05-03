package org.xjcraft;


import com.google.gson.Gson;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.xjcraft.annotation.RCommand;
import org.xjcraft.api.CommonCommandExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 这里展示了如何快速配置一个指令，需要实现CommonCommandExecutor，没有实际功能只是用于区分
 */
public class ExampleCommonCommand implements CommonCommandExecutor {

    private CommonPlugin plugin;

    public ExampleCommonCommand(CommonPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 这是用于展示的指令代码块
     * 再注解中可以配置这个指令的关键字，运行
     *
     * @params 变量会自动注入，除了Command与CommandSender的变量会按照顺序由指令来注入，
     * 比如这个指令的运行方式是/config snexample <player> <text>
     * 当前支持注入的变量包含Player Integer Double String Boolean
     */
    @RCommand(value = "example", sender = RCommand.Sender.CONSOLE, permisson = "example.permission", desc = "示例指令，无作用")
    public void example(Command command, CommandSender sender, Player player, String text) {
        //do something

    }

    /**
     * 通用的保存指令
     * /config save <插件名>    保存指定插件的配置文件
     *
     * @param sender
     * @param plugin
     */
    @RCommand(value = "save", sender = RCommand.Sender.ALL, desc = "保存指定插件的配置文件")
    public void save(CommandSender sender, String plugin) {
        save(sender, plugin, true);
    }

    /**
     * 通用的保存指令
     * /config dump <插件名>    保存指定插件的配置文件,不包含注释
     *
     * @param sender
     * @param plugin
     */
    @RCommand(value = "dump", sender = RCommand.Sender.ALL, desc = "保存指定插件的配置文件,不包含注释")
    public void dump(CommandSender sender, String plugin) {
        save(sender, plugin, false);
    }

    public void save(CommandSender sender, String plugin, boolean hasComment) {
        CommonPlugin commonPlugin = (CommonPlugin) this.plugin.getServer().getPluginManager().getPlugin(plugin);
        if (commonPlugin != null && CommonPlugin.configs.containsKey(plugin)) {
            List<Class> o = CommonPlugin.configs.get(plugin);
            for (Class config : o) {
                try {
                    commonPlugin.saveConfig(config, hasComment);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            sender.sendMessage("保存成功！");
        } else {
            sender.sendMessage("插件不存在！支持保存配置的插件：" + new Gson().toJson(CommonPlugin.configs.keySet()));
        }
    }

    /**
     * 通用的加载指令
     * /config load <插件名>    加载指定插件的配置文件
     *
     * @param sender
     * @param pluginName1
     */
    @RCommand(value = "load", sender = RCommand.Sender.ALL, desc = "加载指定插件的配置文件")
    public void load(CommandSender sender, String pluginName1) {
        CommonPlugin commonPlugin1 = (CommonPlugin) this.plugin.getServer().getPluginManager().getPlugin(pluginName1);
        if (commonPlugin1 != null && CommonPlugin.configs.containsKey(pluginName1)) {
            if (!commonPlugin1.preReload()) {
                sender.sendMessage("加载失败 ！");
                return;
            } else {
                sender.sendMessage("加载成功！");
            }
            List<Class> o = CommonPlugin.configs.get(pluginName1);
            for (Class config : o) {
                try {
                    commonPlugin1.loadConfig(config);
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
            if (!commonPlugin1.onReload()) {
                sender.sendMessage("加载失败 ！");
            } else {
                sender.sendMessage("加载成功！");
            }
        } else {
            sender.sendMessage("插件不存在！支持加载配置的插件：" + new Gson().toJson(CommonPlugin.configs.keySet()));
        }
        sender.sendMessage("PS：本功能为通用重载，主要用于文本类配置的重载，需要插件自行适配重载功能！");
        sender.sendMessage("PS2：替换插件会大概率导致加载错误！插件更新请重启！！！");
    }

    @RCommand(value = "grep", sender = RCommand.Sender.ALL, desc = "显示插件列表")
    public void grep(CommandSender sender) {
        grep(sender, "");
    }

    @RCommand(value = "grep", sender = RCommand.Sender.ALL, desc = "显示包含指定关键字的指令列表")
    public void grep(CommandSender sender, String pluginName) {
        sender.sendMessage(grep(pluginName));
    }

    /**
     * @param key
     * @return
     */
    private String grep(String key) {
        String list = "";
        List<Plugin> plugins = new ArrayList<>();
        plugins.addAll(Arrays.asList(plugin.getServer().getPluginManager().getPlugins()));
        plugins.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        for (Plugin plugin : plugins) {
            String name = plugin.getDescription().getFullName();
            if (name.toLowerCase().contains(key.toLowerCase())) {
                int index = name.toLowerCase().indexOf(key.toLowerCase());
                String newName = "" + (plugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED);
                newName += name.substring(0, index);
                newName += ChatColor.BLUE;
                newName += name.substring(index, index + key.length());
                newName += (plugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED);
                newName += name.substring(index + key.length());
                newName += "\n";
                list += newName;
            }
        }
        return list;
    }
}
