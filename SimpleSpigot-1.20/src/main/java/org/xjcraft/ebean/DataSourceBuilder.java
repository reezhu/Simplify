package org.xjcraft.ebean;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import io.ebeaninternal.api.SpiEbeanServer;
import io.ebeaninternal.dbmigration.DdlGenerator;
import io.ebeaninternal.dbmigration.model.CurrentModel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.xjcraft.config.MysqlConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ree on 2017/7/19.
 */
public class DataSourceBuilder {
    /**
     * The EbeanServer object being built
     */
    private EbeanServer ebeanServer;

    private HikariConfig config = new HikariConfig();

    /**
     * The name of the database.
     * Used internally by eBean and to name SQLite DBs.
     */
    @Getter
    private String label;

    Boolean debug;
    /**
     * The model classes to be registered with the ORM
     */
    @Getter
    @Setter
    private List<Class<?>> classes;


    /**
     * Start building an Ebean object using the default SQLite setup Bukkit would for a typical plugin.
     * You may override the defaults before calling build().
     */
    public DataSourceBuilder(String label, MysqlConfig config) {
        // Basic configuration

        this.label = label;
        this.config.setPoolName(label);
        this.config.setDriverClassName(config.getDataSource_driver());
        this.config.setJdbcUrl(config.getDataSource_url());
        this.config.setUsername(config.getDataSource_username());
        this.config.setPassword(config.getDataSource_password());
        this.config.setMaximumPoolSize(config.getDataSource_maximumPoolSize());
        this.config.setMinimumIdle(config.getDataSource_minimumIdle());
        this.config.setConnectionTimeout(config.getDataSource_connectionTimeOut());
        this.config.setAutoCommit(config.getDataSource_autocommit());
        this.config.addDataSourceProperty("cachePrepStmts", "true");
        this.config.addDataSourceProperty("prepStmtCacheSize", "250");
        this.config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.classes = new ArrayList<Class<?>>();
    }

    public DataSource buildDataSource() {
        if (this.config.getDataSource() != null) return this.config.getDataSource();
        if (debug)
            Bukkit.getServer().getLogger().warning("初始化连接池：" + this.label);
        // DataSource setup
        DataSource ds = new HikariDataSource(config);
        this.config.setDataSource(ds);
        if (this.config.getDataSource() == null) {
            throw new IllegalArgumentException("插件：" + this.label + "无法建立数据库连接！请检查配置文件！");
        }
        if (debug)
            Bukkit.getServer().getLogger().warning("连接池\"" + this.label + "\"初始化成功！");
        return config.getDataSource();
    }

    private ServerConfig serverConfig;

    private ServerConfig getServerConfig() {
        if (serverConfig == null) {
            serverConfig = new ServerConfig();
            serverConfig.setDefaultServer(false);
            serverConfig.setName(this.label);
            serverConfig.setRegister(false);
            serverConfig.setClasses(this.classes);
            serverConfig.setDataSource(buildDataSource());
        }
        return serverConfig;
    }

    /**
     * Build the EbeanServer object and return it for usage
     *
     * @return Ebean object, ready for usage
     */
    public EbeanServer buildEbean() {
        ServerConfig serverConfig = getServerConfig();
        serverConfig.setClasses(this.classes);
        // Engage!
        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        this.ebeanServer = EbeanServerFactory.create(serverConfig);
        Thread.currentThread().setContextClassLoader(previousCL);
        return this.ebeanServer;
    }


    /**
     * Generate database table structure based on registered entity classes
     */
    public void installDDL() {
        DdlGenerator gen = new DdlGenerator((SpiEbeanServer) ebeanServer, getServerConfig());
        CurrentModel method = new CurrentModel((SpiEbeanServer) ebeanServer);
        try (Connection connection = ebeanServer.getPluginApi().getDataSource().getConnection()) {
            String createDdl = method.getCreateDdl();
            if (debug) {
                Bukkit.getServer().getLogger().info("================================= query start =================================");
                Bukkit.getServer().getLogger().info(createDdl);
                Bukkit.getServer().getLogger().info("================================= query   end =================================");
            }
            gen.runScript(connection, true, createDdl, "createTable");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void addClasses(List<Class<?>> classes) {
        this.classes.addAll(classes);
    }
}
