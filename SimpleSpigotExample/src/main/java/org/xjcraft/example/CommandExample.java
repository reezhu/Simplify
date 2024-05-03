package org.xjcraft.example;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.xjcraft.annotation.RCommand;
import org.xjcraft.api.CommonCommandExecutor;

public class CommandExample implements CommonCommandExecutor {
    private final Plugin plugin;

    public CommandExample(Plugin plugin) {
        this.plugin = plugin;
    }

    //可以根据变量自动生成子命令，如这个指令是/example test1 <Player> <Integer> <String>并且只能由玩家执行
    @RCommand(value = "test1", sender = RCommand.Sender.PLAYER,desc = "指令描述")
    public void test(CommandSender player, Player target, Integer value, String value2) {
        target.sendMessage("Hello World! from" + ((Player) player).getDisplayName());
        target.sendMessage(String.format("param is %s,%s", value, value2));
    }
}
