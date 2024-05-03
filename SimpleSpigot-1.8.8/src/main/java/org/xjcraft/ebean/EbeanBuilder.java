package org.xjcraft.ebean;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.SQLitePlatform;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;
import com.avaje.ebeaninternal.server.lib.sql.TransactionIsolation;
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
        setName(plugin.toString());
        setDriver("org.sqlite.JDBC");
        setURL("jdbc:sqlite://" + file.getAbsolutePath());
        setCredentials("bukkit", "walrus"); //bukkit defaults
        setIsolationLevel("SERIALIZABLE");
        setDependent(true);

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
        ServerConfig config = new ServerConfig();
        config.setDefaultServer(false);
        config.setName(this.name);
        config.setRegister(false);
        config.setClasses(this.classes);
        config.setResourceDirectory(plugin.getDataFolder().toString());


        // SQLite-specific handling
        if (this.driver.contains("sqlite")) {
            config.setDatabasePlatform(new SQLitePlatform());
            config.getDatabasePlatform().getDbDdlSyntax().setIdentity("");
            File file = new File(plugin.getDataFolder(), this.name + ".db");
            file.getParentFile().mkdirs();
        }
        Object dataSource = null;
//        if (Bukkit.getPluginManager().isPluginEnabled("SpigotDBPool")) {
//            dataSource = DataManage.getDataSource(this.name);
//            if (dataSource == null) {
//                plugin.getLogger().warning("插件：" + this.name + "无法获取SpigotDBPool连接池！将使用独立配置！");
//            } else {
//                config.setDataSource((DataSource) dataSource);
//            }
//        }


        if (dataSource == null) {
            // DataSource setup
            DataSourceConfig ds = new DataSourceConfig();
            ds.setDriver(this.driver);
            ds.setUrl(this.url);
            ds.setUsername(this.username);
            ds.setPassword(this.password);
            ds.setIsolationLevel(this.isolationLevel);
            ds.setMaxConnections(20);
            config.setDataSourceConfig(ds);
        }

        // Engage!
        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(plugin.getClass().getClassLoader());
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
        this.isolationLevel = TransactionIsolation.getLevel("SERIALIZABLE");
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

    public EbeanBuilder setDependent(boolean dependent) {
        return this;
    }

    /**
     * Generate database table structure based on registered entity classes
     */
    public void installDDL() {
        SpiEbeanServer serv = (SpiEbeanServer) ebeanServer;
        DdlGenerator gen = serv.getDdlGenerator();
        gen.runScript(false, gen.generateCreateDdl());
    }

    /**
     * Generate database table structure based on registered entity classes
     */
    public static void installDDL(EbeanServer ebeanServer) {
        SpiEbeanServer serv = (SpiEbeanServer) ebeanServer;
        DdlGenerator gen = serv.getDdlGenerator();
        gen.runScript(false, gen.generateCreateDdl());
    }


    /**
     * Trash the database
     */
    public void removeDDL() {
        SpiEbeanServer serv = (SpiEbeanServer) ebeanServer;
        DdlGenerator gen = serv.getDdlGenerator();
        gen.runScript(true, gen.generateDropDdl());
    }
}
