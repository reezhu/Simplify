package org.xjcraft.bean;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;
import org.xjcraft.annotation.Comment;
import org.xjcraft.api.SimpleConfigurationSerializable;

import java.util.Map;

@Data
@NoArgsConstructor
public class IconBundle implements SimpleConfigurationSerializable {
    @Comment("格子位置")
    Integer slot;
    @Comment("鼠标默认时的图标")
    ItemStack hover;
    @Comment("鼠标选中时的图标")
    ItemStack pressed;
    @Comment("按钮类型")
    ButtonType type;
    @Comment("按钮功能")
    String function;
    @Comment("附加信息")
    Map<String, Object> addition;

    public IconBundle(Integer slot, ItemStack hover, ItemStack pressed, ButtonType type, String function, Map<String, Object> addition) {
        this.slot = slot;
        this.hover = hover;
        this.pressed = pressed;
        this.type = type;
        this.function = function;
        this.addition = addition;
    }

    public enum ButtonType {
        SINGLE, REPEAT, REFRESH
    }
}
