package org.xjcraft;

import org.xjcraft.adapter.LoggerApi;

/**
 * Created by Ree on 2017/8/19.
 */
public class SimpleSpigot extends CommonPlugin {
    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        //根据注解加载config文件夹下所有的配置文件
        loadConfigs();
        //此方法用于注册自动注入指令
        registerCommand(new ExampleCommonCommand(this));
    }


}
