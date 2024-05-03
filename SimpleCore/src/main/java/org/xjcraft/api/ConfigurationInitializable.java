package org.xjcraft.api;

public interface ConfigurationInitializable {
    /**
     * 注意这个接口会在单独配置加载完成后即调用，不能保证其他配置文件的可靠性
     *
     * @return 是否成功
     */
    boolean onLoaded();
}
