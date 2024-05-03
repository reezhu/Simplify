package org.xjcraft.ebean;

import com.avaje.ebean.EbeanServer;
import org.xjcraft.CommonPlugin;
import org.xjcraft.config.MysqlConfig;

/**
 * Created by Ree on 2017/8/4.
 */
public class EbeanServerBuilder {

    public static EbeanServer setupDatabase(MysqlConfig config, CommonPlugin plugin) {

        EbeanBuilder builder = null;
        EbeanServer database = null;
        try {
            plugin.getLogger().info("Trying to enable database...");
            builder = new EbeanBuilder(plugin)
                    .setName(plugin.getName())
                    .setDriver(config.getDataSource_driver())
                    .setURL(config.getDataSource_url())
                    .setCredentials(config.getDataSource_username(), config.getDataSource_password())
                    .setClasses(plugin.getDatabaseClasses(plugin));

            database = builder.build();
            for (Class<?> aClass : builder.getClasses()) {
                database.find(aClass).setMaxRows(1).findUnique();
            }
            plugin.getLogger().info("Database enable successful!");
        } catch (NullPointerException e) {
            plugin.getLogger().warning("Database config missing！Please check your config！");
            e.printStackTrace();
        } catch (Exception e) {
            plugin.getLogger().info("Fail to enable database, trying to initialize...");
            try {
                builder.installDDL();
//                installDDL();
                plugin.getLogger().info("Successful import database structure.");
            } catch (Exception e2) {
                plugin.getLogger().warning("Fail to create database structure, please make sure the clear of database!");
                e2.printStackTrace();
            }
        }
        return database;
    }


}
