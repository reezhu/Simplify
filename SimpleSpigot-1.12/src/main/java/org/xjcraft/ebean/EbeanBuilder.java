package org.xjcraft.ebean;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import io.ebeaninternal.api.SpiEbeanServer;
import io.ebeaninternal.dbmigration.DdlGenerator;
import io.ebeaninternal.dbmigration.model.CurrentModel;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ree on 2017/7/19.
 */
public class EbeanBuilder {
    /**
     * The EbeanServer object being built
     */
    private EbeanServer ebeanServer;

    private ServerConfig config;
    /**
     * The plugin this database is for
     */
    private Plugin plugin;


    /**
     * The name of the database.
     * Used internally by eBean and to name SQLite DBs.
     */
    private String name;


    /**
     * The database driver
     */
    private String driver;


    /**
     * The JDBC URL for the database
     */
    private String url;


    /**
     * Database username.
     * SQLite still requires one be set, though its value doesn't matter.
     */
    private String username;


    /**
     * Database password.
     * SQLite still requires one be set, though its value doesn't matter.
     */
    private String password;


    /**
     * The model classes to be registered with the ORM
     */
    private List<Class<?>> classes;


    /**
     * Isolation level
     */
    private int isolationLevel;


    /**
     * Start building an Ebean object using the default SQLite setup Bukkit would for a typical plugin.
     * You may override the defaults before calling build().
     *
     * @param plugin the plugin this database belongs to
     */
    public EbeanBuilder(Plugin plugin) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), plugin.toString() + ".db");
        setName(plugin.getName());
        setDriver("org.sqlite.JDBC");
        setURL("jdbc:sqlite://" + file.getAbsolutePath());
        setCredentials("bukkit", "walrus"); //bukkit defaults
        setIsolationLevel("SERIALIZABLE");


        this.classes = new ArrayList<Class<?>>();
    }


    /**
     * Build the EbeanServer object and return it for usage
     *
     * @return Ebean object, ready for usage
     */
    public EbeanServer build() {
        init();
        return this.ebeanServer;
    }


    /**
     * Initialize the EbeanServer object from the stored settings
     */
    private void init() {

        // Basic configuration

        config = new ServerConfig();
        config.setDefaultServer(false);
        config.setName(this.name);
        config.setRegister(false);
        config.setClasses(this.classes);
        config.setResourceDirectory(plugin.getDataFolder().toString());


//        if (plugin.getServer().getPluginManager().getPlugin("NukkitMysqlPool") != null) {
//            DataSource dataSource = DataManage.getDataSource(this.name);
//            if (dataSource == null) {
//                plugin.getLogger().warning("插件：" + this.name + "无法获取NukkitMysqlPool连接池！将被禁用！");
//                plugin.getServer().getPluginManager().disablePlugin(plugin);
//                throw new IllegalArgumentException("插件：" + this.name + "无法获取NukkitMysqlPool连接池！");
//            } else {
//                config.setDataSource(dataSource);
//                plugin.getLogger().info("插件：" + this.name + "已使用NukkitMysqlPool连接池！");
//            }
//        } else
        {
//            plugin.getLogger().warning("插件：NukkitMysqlPool不存在！将使用独立配置！");
            // DataSource setup
            HikariConfig hconfig = new HikariConfig();
            hconfig.setDriverClassName(this.driver);
            hconfig.setJdbcUrl(this.url);
            hconfig.setUsername(this.username);
            hconfig.setPassword(this.password);
//            hconfig.setIsolateInternalQueries(this.isolationLevel);
            hconfig.setMaximumPoolSize(20);
            hconfig.setConnectionTimeout(1500);
            hconfig.setMinimumIdle(4);
            hconfig.addDataSourceProperty("cachePrepStmts", "true");
            hconfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hconfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            HikariDataSource ds = new HikariDataSource(hconfig);
            config.setDataSource(ds);
            plugin.getLogger().warning("插件：" + this.name + "已使用独立连接池！");
        }


        if (config.getDataSource() == null) {
            throw new IllegalArgumentException("插件：" + this.name + "无法建立数据库连接！请检查配置文件！");
        }

        // Engage!
        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        this.ebeanServer = EbeanServerFactory.create(config);
        Thread.currentThread().setContextClassLoader(previousCL);

    }


    /**
     * Set the name of the database.
     * Ebean will use this internally, and it will be used as the filename for SQLite DBs.
     *
     * @param name the name
     */
    public EbeanBuilder setName(String name) {
        this.name = name;
        return this;
    }


    /**
     * Set the JDBC driver class. e.g. "org.sqlite.JDBC" for SQLite
     *
     * @param driver driver class reverse URL
     */
    public EbeanBuilder setDriver(String driver) {
        this.driver = driver;
        return this;
    }


    /**
     * Set the JDBC URL for the database.
     *
     * @param url JDBC URL
     */
    public EbeanBuilder setURL(String url) {
        this.url = url;
        return this;
    }


    /**
     * Set the credentials to connect to the database.
     * Ebean expects these to be set for SQLite, even though the values don't matter.
     *
     * @param username username for the database
     * @param password password for the database
     * @return
     */
    public EbeanBuilder setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }


    /**
     * Set the transaction isolation level. Defaults to "SERIALIZABLE"
     *
     * @param isolationLevel e.g. "SERIALIZABLE"
     */
    public EbeanBuilder setIsolationLevel(String isolationLevel) {
//        this.isolationLevel = TransactionIsolation.getLevel("SERIALIZABLE");
        return this;
    }


    /**
     * Register the model classes with Ebean
     *
     * @param classes list of classes that will be Ebean models
     */
    public EbeanBuilder setClasses(List<Class<?>> classes) {
        this.classes = classes;
        return this;
    }

    public List<Class<?>> getClasses() {
        return this.classes;
    }


    /**
     * Generate database table structure based on registered entity classes
     */
    public void installDDL() {
        DdlGenerator gen = new DdlGenerator(SpiEbeanServer.class.cast(ebeanServer), config);
        CurrentModel method = new CurrentModel(SpiEbeanServer.class.cast(ebeanServer));
        try {
            String createDdl = method.getCreateDdl();
            plugin.getLogger().info("================================= query start =================================");
            plugin.getLogger().info(createDdl);
            plugin.getLogger().info("================================= query   end =================================");
            gen.runScript(true, createDdl, "createTable");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }


    /**
     * Trash the database
     */
    public void removeDDL() {
        DdlGenerator gen = new DdlGenerator(SpiEbeanServer.class.cast(ebeanServer), config);
        CurrentModel method = new CurrentModel(SpiEbeanServer.class.cast(ebeanServer));
        //TODO
        /*try {
            gen.runScript(true,method.getDropAllDdl(),);

        } catch (IOException e1) {
            e1.printStackTrace();
        }*/
    }
}
