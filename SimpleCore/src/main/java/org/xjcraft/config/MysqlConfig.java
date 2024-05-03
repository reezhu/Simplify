package org.xjcraft.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.xjcraft.annotation.Comment;
import org.xjcraft.annotation.Folder;
import org.xjcraft.annotation.RConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ree on 2017/8/7.
 */

@RConfig(value = "database")
@Data
@NoArgsConstructor
public class MysqlConfig {
    //文件夹类配置的注解，当采用此注解时类型需配置key为String（用来存文件名）的一个map，为空时会根据本类的默认配置生成一份example.yml文件
    @Folder
    //配置类需设置为static保证单例，推荐使用public static final，变量名由于历史原因请设置成config
    public static final Map<String, MysqlConfig> config = new HashMap<>();

    @Comment("数据库debug信息")
    Boolean debugInfo = true;
    @Comment("启动类")
    String dataSource_driver = "com.mysql.jdbc.Driver";
    @Comment("连接地址")
    String dataSource_url = "jdbc:mysql://localhost:3306/minecraft";
    @Comment("用户名")
    String dataSource_username = "minecraft";
    @Comment("密码")
    String dataSource_password = "minecraft";
    @Comment("最大连接池")
    Integer dataSource_maximumPoolSize = 20;
    @Comment("最小闲置连接数")
    Integer dataSource_minimumIdle = 4;
    @Comment("超时时间")
    Integer dataSource_connectionTimeOut = 1500;
    @Comment("是否自动提交")
    Boolean dataSource_autocommit = true;
    @Comment("别名")
    List<String> alias = new ArrayList<>();

//    @Ignore
//    public static final Map<String, DataSource> dataSources = new HashMap<>();
//
//    public static void init() {
//
//        Bukkit.getServer().getLogger().info("rearrange the data sources.....");
//        dataSources.clear();
//        for (Map.Entry<String, MysqlConfig> entry : config.entrySet()) {
//            MysqlConfig value = entry.getValue();
//            DataSourceBuilder builder = new DataSourceBuilder(entry.getKey(), value);
//            DataSourceBuilder put = dataSources.put(entry.getKey(), builder);
//            if (put != null) {
//                Bukkit.getServer().getLogger().warning(entry.getKey() + ".yml is redundant!");
//            }
//            for (String alias : value.alias) {
//                DataSourceBuilder put1 = dataSources.put(alias, builder);
//                if (put1 != null) {
//                    Bukkit.getServer().getLogger().warning(alias + "in " + entry.getKey() + ".yml is redundant!");
//                }
//            }
//        }
//        Bukkit.getServer().getLogger().info("finished rearrange the data sources.");
//    }
}
