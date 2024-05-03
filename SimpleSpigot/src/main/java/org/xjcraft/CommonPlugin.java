package org.xjcraft;

import com.google.gson.internal.Primitives;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.xjcraft.adapter.LoggerApi;
import org.xjcraft.adapter.Pluggable;
import org.xjcraft.annotation.*;
import org.xjcraft.api.CommonCommandExecutor;
import org.xjcraft.api.ConfigurationInitializable;
import org.xjcraft.manager.CommandManager;
import org.xjcraft.parsers.CommentConfig;
import org.xjcraft.parser.ExcelParser;
import org.xjcraft.utils.ReflectUtil;
import org.xjcraft.utils.SerializeUtil;
import org.xjcraft.utils.StringUtil;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ree on 2017/8/4.
 */
public abstract class CommonPlugin extends JavaPlugin implements Pluggable {
    protected static Map<String, List<Class>> configs = new HashMap<>();
    protected CommandManager commandManager = new CommandManager(this);

    protected boolean preReload() {
        return true;
    }

    protected boolean onReload() {
        return true;
    }

    public void registerCommand(CommonCommandExecutor executor) {
        commandManager.register(executor);
    }
    protected DataSource getDataSource(String label) {
        //todo
        return null;
    }
    /**
     * 注册根指令
     * @param command
     * @param executor
     */
    public void registerMainCommand(String command, CommonCommandExecutor executor) {
        Bukkit.getServer().getPluginCommand(command).setExecutor(this);
        commandManager.register(executor);
    }


    /**
     * 反射的读取所有配置
     */
    protected void loadConfigs() {
        for (Class<?> config : getClasses("config")) {
            if (config.isAnnotationPresent(RConfig.class))
                loadConfig(config);
        }
    }


    /**
     * 加载所有插件config目录下的配置，支持@Instance与@Folder还有@Excel
     *
     * @param clazz
     */
    protected void loadConfig(Class clazz) {
        try {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        if (field.isAnnotationPresent(Ignore.class)) {
                            continue;
                        } else if (field.isAnnotationPresent(Instance.class)) {
                            field.setAccessible(true);
                            loadConfig(field.get(null));
                            field.setAccessible(false);
                        } else if (field.isAnnotationPresent(Folder.class)) {
                            field.setAccessible(true);
                            loadFolder(clazz, field);
                            field.setAccessible(false);
                        } else if (field.isAnnotationPresent(Excel.class)) {
                            field.setAccessible(true);
                            loadExcel(clazz, field.get(null));
                            field.setAccessible(false);

                        }
                    } catch (ClassCastException e) {
                        getLogger().warning(clazz.getName() + " KEY ELEMENT MISMATCH! 关键元素错误！");
                        e.printStackTrace();
                    } catch (Exception e) {
                        getLogger().warning(clazz.getName() + " ConfigLoad Error!!!配置读取错误！！！");
                        e.printStackTrace();
                        if (e instanceof RuntimeException) throw (RuntimeException) e;
                    }
                }
            }
        } finally {
            if (!configs.containsKey(this.getName()))
                configs.put(this.getName(), new ArrayList<>());
            List<Class> list = configs.get(this.getName());
            if (!list.contains(clazz))
                list.add(clazz);
        }


    }

    private void loadExcel(Class<?> clazz, Object bean) {
        RConfig rc = clazz.getAnnotation(RConfig.class);
        String configName = rc.value();
        {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }

            File file = new File(getDataFolder(), configName);
            if (!file.exists()) {
                saveConfig(clazz, true);
            }

            try {
                ExcelParser.loadExcel(this, clazz, new FileInputStream(file));
                if (bean instanceof ConfigurationInitializable) {
                    boolean b = ((ConfigurationInitializable) bean).onLoaded();
                    if (!b)
                        getLogger().warning(this.getName() + "/" + configName + "配置初始化错误，请留意提示或联系相关技术查看配置问题！");
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("读取excel文件" + configName + "失败！");
            }
        }


    }

    private void loadFolder(Class clazz, Field field) throws IllegalAccessException, InstantiationException, IOException {
        field.setAccessible(true);
        Map<String, Object> o = (Map<String, Object>) field.get(null);
        field.setAccessible(false);
        String name = ((RConfig) clazz.getAnnotation(RConfig.class)).value();
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();

        }
        String path = getDataFolder().getPath() + File.separator + name;
        File subFolder = new File(path);
        if (!subFolder.exists()) {
            subFolder.mkdir();
            if (o.size() == 0)
                o.put("example.yml", clazz.newInstance());
            for (Map.Entry<String, Object> entry : o.entrySet()) {
                String file = entry.getKey();
                Object value = entry.getValue();
                saveConfig(value, clazz, subFolder.getName() + File.separator + file, true);
            }

            return;
        }
        o.clear();
        for (File file : subFolder.listFiles()) {
            Object value = clazz.newInstance();
            String key = file.getName().replace(".yml", "");
            o.put(key, value);
            loadConfig(name + File.separator + file.getName(), clazz, value);
        }
    }

    private Object loadConfig(Object bean) {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        Class clazz = bean.getClass();
        String configName = ((RConfig) clazz.getAnnotation(RConfig.class)).value();
        loadConfig(configName, clazz, bean);
        return bean;
    }

    private void loadConfig(String configName, Class clazz, Object bean) {
        try {
            File file = new File(getDataFolder(), configName);
            if (!file.exists()) {
                Field field = clazz.getField("config");
                field.setAccessible(true);
                Object o = field.get(bean);
                field.setAccessible(false);
                saveConfig(o);
            }
            YamlConfiguration configuration = new CommentConfig(new File(getDataFolder(), configName));
            Field[] fields = (Field[]) ArrayUtils.addAll(clazz.getSuperclass().getDeclaredFields(), clazz.getDeclaredFields());
            for (Field field : fields) {
                if (field.isAnnotationPresent(Ignore.class) || field.isAnnotationPresent(Instance.class) || field.isAnnotationPresent(Folder.class)) {
                    continue;
                } else {
                    String name = field.getName();
                    Object o = configuration.get(name.replaceAll("_", "."));
                    if (o == null) {
                        continue;
                    } else if (o instanceof String && StringUtil.isEmpty((String) o)) {
//                        continue;
                    } else if (field.isAnnotationPresent(Text.class)) {
                        if (field.getType() == String.class)
                            o = ((String) o).replaceAll("&", "§");
                        if (field.getType() == List.class) {
                            for (int i = 0; i < ((List<String>) o).size(); i++) {
                                ((List<String>) o).set(i, ((List<String>) o).get(i).replaceAll("&", "§"));
                            }
                        }
                    } else if (o instanceof Map || o instanceof List || o instanceof MemorySection) {
                        o = SerializeUtil.deSerialize(o);
                    } else if (field.getType().isEnum()) {
                        try {
                            o = Enum.valueOf((Class<Enum>) field.getType(), (String) o);
                        } catch (IllegalArgumentException e) {
                            String s = "名为" + o + "的枚举类不存在,可用类型有：";
                            for (Object constant : field.getType().getEnumConstants()) {
                                s += constant;
                                s += ";";
                            }
                            getLogger().warning(s);
                            throw e;
                        }
                    }
                    o = fixClassTypeMismatch(configName, field, o);
                    try {
                        field.setAccessible(true);
                        field.set(bean, o);
                        field.setAccessible(false);
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("不支持的属性：" + field.getName() + ";类型" + field.getType());
                        e.printStackTrace();
                    }
                }
            }
            if (bean instanceof ConfigurationInitializable) {
                boolean b = ((ConfigurationInitializable) bean).onLoaded();
                if (!b) throw new RuntimeException(this.getName() + "/" + configName + "配置初始化错误，请留意提示或联系相关技术查看配置问题！");
            }
        } catch (Exception e) {
            getLogger().warning(configName + " 配置读取错误！！！");
            e.printStackTrace();
        }
    }

    /**
     * 单独保存某个class的配置
     *
     * @param config
     */
    public void saveConfig(Class config) {
        saveConfig(config, true);
    }

    /**
     * 新版保存接口，支持@Instance与@Folder还有@Excel
     *
     * @param clazz
     */
    protected void saveConfig(Class<?> clazz, boolean hasComment) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();

        }

        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    if (field.isAnnotationPresent(Ignore.class)) {
                        continue;
                    } else if (field.isAnnotationPresent(Instance.class)) {
                        field.setAccessible(true);
                        Object o = field.get(null);
                        field.setAccessible(false);
                        saveConfig(o);
                    } else if (field.isAnnotationPresent(Folder.class)) {
                        Map<String, Object> o = (Map<String, Object>) field.get(null);
                        String name = ((RConfig) clazz.getAnnotation(RConfig.class)).value();
                        String path = getDataFolder().getPath() + File.separator + name;
                        File subFolder = new File(path);
                        if (!subFolder.exists()) {
                            subFolder.mkdir();
                        }
                        for (Map.Entry<String, Object> entry : o.entrySet()) {
                            String s = name + File.separator + entry.getKey() + ".yml";
                            saveConfig(entry.getValue(), clazz, s, hasComment);
                        }
                    } else if (field.isAnnotationPresent(Excel.class)) {
                        RConfig rConfig = clazz.getAnnotation(RConfig.class);
                        String configName = rConfig.value();
                        {
                            String path = getDataFolder().getPath() + File.separator + configName;
                            File file = new File(path);
                            ExcelParser.saveExcel(this, clazz, new FileOutputStream(file));
                        }
                    }
                } catch (ClassCastException e) {
                    getLogger().warning(clazz.getName() + " KEY ELEMENT MISMATCH! 关键元素错误！");
                    e.printStackTrace();
                } catch (Exception e) {
                    getLogger().warning(clazz.getName() + " ConfigSave Error!!!配置保存错误！！！");
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveConfig(Object bean) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class clazz = bean.getClass();
        String configName = ((RConfig) clazz.getAnnotation(RConfig.class)).value();
        saveConfig(bean, clazz, configName, true);
    }

    private void saveConfig(Object bean, Class clazz, String configName, boolean hasComment) throws IOException, IllegalAccessException {
        File file = new File(getDataFolder(), configName);
        if (!file.exists()) {
            file.createNewFile();
        }
        CommentConfig configuration = new CommentConfig(new File(getDataFolder(), configName));
        Field[] fields = (Field[]) ArrayUtils.addAll(clazz.getSuperclass().getDeclaredFields(), clazz.getDeclaredFields());
        for (Field field : fields) {
            if (field.isAnnotationPresent(Ignore.class) || field.isAnnotationPresent(Instance.class) || field.isAnnotationPresent(Folder.class)) {
                continue;
            } else {
                String comment = null;
                if (field.isAnnotationPresent(Comment.class)) {
                    Comment annotation = field.getAnnotation(Comment.class);
                    comment = annotation.value();
                }
                String name = field.getName();
                String key = name.replaceAll("_", ".");
                field.setAccessible(true);
                Object o = field.get(bean);
                field.setAccessible(false);
                o = SerializeUtil.serialize(this,o, field, key, configuration);
                configuration.set(key, o, comment);
            }
        }
        configuration.saveCommentConfig(file, hasComment);
    }


    /**
     * 尝试修复错误的类型并提示
     *
     * @param className
     * @param field
     * @param value
     * @return
     */
    private Object fixClassTypeMismatch(String className, Field field, Object value) {
        if (value != null && Primitives.wrap(field.getType()) != Primitives.wrap(value.getClass())
                && (Primitives.isWrapperType(value.getClass()) || value.getClass() == String.class)
        ) {
            switch (Primitives.wrap(field.getType()).getSimpleName()) {
                case "Double":
                    getLogger().warning(String.format("%s中的%s不是%s而是%s", className, field.getName(), value.getClass().getSimpleName(), field.getType().getSimpleName()));
                    value = new BigDecimal(value.toString()).doubleValue();
                    break;
                case "Integer":
                    getLogger().warning(String.format("%s中的%s不是%s而是%s", className, field.getName(), value.getClass().getSimpleName(), field.getType().getSimpleName()));
                    value = new BigDecimal(value.toString()).intValue();
                    break;
                case "String":
                    getLogger().warning(String.format("%s中的%s不是%s而是%s", className, field.getName(), value.getClass().getSimpleName(), field.getType().getSimpleName()));
                    value = value.toString();
                    break;
                default:
                    break;
            }

        }
        return value;
    }

    public List<Class<?>> getDatabaseClasses(Plugin plugin) {
        return getClasses("entity");
    }

    public List<Class<?>> getModelClasses(Plugin plugin) {
        return getClasses("model");
    }

    /**
     * 获取本插件某个目录下的所有类
     *
     * @param packet
     * @return
     */
    public List<Class<?>> getClasses(String packet) {
        Class<? extends Plugin> clazz = this.getClass();
        return getClasses(packet, clazz);
    }

    private static List<Class<?>> getClasses(String packet, Class<?> pluginClass) {
        URL url = pluginClass.getProtectionDomain().getCodeSource().getLocation();
        String s = pluginClass.getPackage().getName() + "." + packet;
        s = s.replaceAll("\\.", "/");
        List<Class<?>> classes = new ArrayList<>();
        try {
            File file = Paths.get(url.toURI()).toFile();
            classes.addAll(ReflectUtil.getAllClassesFromPackage(s, file, pluginClass.getClassLoader(), packet));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return classes;
    }

    public static String packageName = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandManager.dispatch(sender, command, label, args);
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public LoggerApi getLoggerApi() {
        return new LoggerApi() {
            @Override
            public void debug(String msg) {
                getLogger().fine(msg);
            }

            @Override
            public void warning(String msg) {
                getLogger().warning(msg);
            }

            @Override
            public void info(String msg) {
                getLogger().info(msg);
            }

            @Override
            public void error(String msg) {
                getLogger().severe(msg);
            }

        };
    }
}
