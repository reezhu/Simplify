package org.xjcraft.example.config;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.xjcraft.annotation.Comment;
import org.xjcraft.annotation.Ignore;
import org.xjcraft.annotation.Instance;
import org.xjcraft.annotation.RConfig;
import org.xjcraft.api.ConfigurationInitializable;
import org.xjcraft.bean.MsgBundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 这个配置文件展示了如何生成一个文件类型的配置
 * Created by Ree on 2017/8/7.
 */
//对应的文件名需要手工配置，默认的文件名为config.yml
@RConfig(value = "config.yml")
@Data
//配置文件可以获取父类的配置，用来实现常用配置的复用，比如DBConfig包含了数据库连接所用的配置
public class ExampleConfig extends ExampleParentConfig implements ConfigurationInitializable {
    //这个注解代表对应单个配置文件
    @Instance
    //配置类需设置为static保证单例，推荐使用public static final，变量名由于历史原因请设置成config
    public static final ExampleConfig config = new ExampleConfig();
    //历史功能，会根据下划线来封装层级，这两个配置会变成一个first里的aaa与bbb两个元素
    String first_aaa = "1历史";
    String first_bbb = "2功能";
    @Comment("支持大部分基础类型，注意需要使用包装类型（代表了配置缺失）")
    Boolean second = true;
    Integer third = 1;
    Long fourth = 1L;
    Double fifth = 1.0;
    Float sixth = 1.0f;

    @Comment("支持值与序列化的值内的枚举类")
    Test enu = Test.AAA;
    @Comment("支持mc自带的ConfigurationSerializable的序列化")
    ItemStack itemStack = new ItemStack(Material.ACACIA_DOOR);
    @Comment("支持SimpleConfigurationSerializable的序列化")
    MsgBundle msg = new MsgBundle("啊", "不", "从", "的");
    @Comment("支持list与map的相互嵌套")
    List<MsgBundle> list = new ArrayList<MsgBundle>() {{
        add(msg);
    }};
    Map<String, MsgBundle> map = new HashMap<>() {{
        put("test", msg);
    }};
    //标记为ignore的字段不会被保存，可以用来对配置进行预处理，方便未来调用
    @Ignore
    Map<String, String> ignored = new HashMap<>();

    @Override
    public boolean onLoaded() {
        //此接口会在配置被重载时调用,false会报错
        ignored.clear();
        for (MsgBundle bundle : list) {
            ignored.put(bundle.getTitle(), bundle.getSubtitle());
        }
        return !ignored.isEmpty();
    }

    public enum Test {
        AAA, BBB
    }


}
