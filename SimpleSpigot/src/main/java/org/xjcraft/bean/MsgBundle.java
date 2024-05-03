package org.xjcraft.bean;

import com.connorlinfoot.titleapi.TitleAPI;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.xjcraft.annotation.Comment;
import org.xjcraft.api.SimpleConfigurationSerializable;
import org.xjcraft.utils.HotbarMessager;
import org.xjcraft.utils.StringUtil;

import java.util.Collection;
import java.util.HashMap;

@Data
@NoArgsConstructor
public class MsgBundle implements SimpleConfigurationSerializable {
    @Comment("聊天栏")
    String chat;
    @Comment("标题栏")
    String title;
    @Comment("副标题栏")
    String subtitle;
    @Comment("快捷栏")
    String hotbar;
    @Comment("标题渐入时间")
    Integer fadein;
    @Comment("持续时间")
    Integer stay;
    @Comment("标题渐隐时间")
    Integer fadeout;

    public MsgBundle(String chat, String title, String subtitle, String hotbar, Integer fadein, Integer stay, Integer fadeout) {
        this.chat = chat;
        this.title = title;
        this.subtitle = subtitle;
        this.hotbar = hotbar;
        this.fadein = fadein;
        this.stay = stay;
        this.fadeout = fadeout;
    }

    public MsgBundle(String chat, String title, String subtitle, String hotbar) {
        this.chat = chat;
        this.title = title;
        this.subtitle = subtitle;
        this.hotbar = hotbar;
        fadein = 20;
        stay = 50;
        fadeout = 20;
    }

    public MsgBundle(String chat) {
        this.chat = chat;
        fadein = 10;
        stay = 20;
        fadeout = 10;
    }


    public void broadcast() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            tell(player, new HashMap<String, String>() {{
                put("player", player.getName());
            }});
        }
    }

    public void broadcast(HashMap<String, String> placeHolder) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            tell(player, placeHolder);
        }
    }

    public void tell(Collection<Player> players, HashMap<String, String> placeHolder) {
        for (Player player : players) {
            tell(player, placeHolder);
        }
    }

    public void tell(Player player, HashMap<String, String> placeHolder) {
        if (getChat() != null)
            player.sendMessage(StringUtil.applyPlaceHolder(getChat(), placeHolder));
        if (!StringUtil.isEmpty(getTitle()))
            TitleAPI.sendTitle(player, getFadein(), getStay(), getFadeout(), StringUtil.applyPlaceHolder(getTitle(), placeHolder));
        if (!StringUtil.isEmpty(getSubtitle()))
            TitleAPI.sendSubtitle(player, getFadein(), getStay(), getFadeout(), StringUtil.applyPlaceHolder(getSubtitle(), placeHolder));
        if (!StringUtil.isEmpty(getHotbar()))
            HotbarMessager.sendHotBarMessage(player, StringUtil.applyPlaceHolder(getHotbar(), placeHolder));
    }

}
