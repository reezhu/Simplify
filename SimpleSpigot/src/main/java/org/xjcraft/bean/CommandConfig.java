package org.xjcraft.bean;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.xjcraft.annotation.Comment;
import org.xjcraft.api.SimpleConfigurationSerializable;
import org.xjcraft.utils.StringUtil;

import java.util.*;

/**
 * 指令配置项
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class CommandConfig implements SimpleConfigurationSerializable {
    @Comment("发送者，cosole/player")
    SenderType sender;
    @Comment("指令，默认placeholder为%player%")
    List<String> command;

    public CommandConfig(String... command) {
        this.sender = SenderType.CONSOLE;
        this.command = List.of(command);
    }

    public CommandConfig(SenderType sender, String... command) {
        this.sender = sender;
        this.command = List.of(command);
    }

    public void dispatch(Plugin plugin, Player player) {
        dispatch(plugin, player, new HashMap<String, String>() {{
            put("player", player.getName());
        }});
    }

    public void dispatch(Plugin plugin, Player player, HashMap<String, String> placeHolder) {
        CommandSender cmder;
        if (sender == SenderType.CONSOLE) {
            cmder = plugin.getServer().getConsoleSender();
        } else {
            cmder = player;
        }
        for (String s : command) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> plugin.getServer().dispatchCommand(cmder,
                    StringUtil.applyPlaceHolder(s, placeHolder)));

        }


    }
}
