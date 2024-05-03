package org.xjcraft.utils;

import com.google.gson.internal.Primitives;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.xjcraft.CommonPlugin;
import org.xjcraft.adapter.Pluggable;
import org.xjcraft.annotation.Comment;
import org.xjcraft.annotation.Ignore;
import org.xjcraft.api.SimpleConfigurationSerializable;
import org.xjcraft.parsers.CommentConfig;
import org.xjcraft.utils.iterator.MultiIterator;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerializeUtil {
    private static final String VERSION_LABEL = "$version";

    /**
     * 序列化
     *
     * @param plugin
     * @param o
     * @param field         o的field
     * @param key           o的key
     * @param configuration o的配置
     * @return
     */
    public static Object serialize(Pluggable plugin, Object o, Field field, String key, CommentConfig configuration) {
        if (field.getType().isEnum()) {
            return ((Enum) o).name();
        } else if (o instanceof ConfigurationSerializable) {
            try {
                Map<String, Object> serialize = new LinkedHashMap<String, Object>() {{
                    put("==", o.getClass().getName().replace(plugin.getPackageName(), VERSION_LABEL));
                    putAll(((ConfigurationSerializable) o).serialize());
                }};
                return serialize(plugin, serialize, field, key, configuration);
            } catch (Exception e) {
                Bukkit.getServer().getLogger().warning(o.getClass().getName() + "序列化失败！");
                throw e;
            }
        } else if (o instanceof SimpleConfigurationSerializable) {
            try {
                return toMap(plugin,o, key, configuration);
            } catch (Exception e) {
                Bukkit.getServer().getLogger().warning(o.getClass().getName() + "序列化失败！");
                throw e;
            }
        } else if (o instanceof Map) {
            LinkedHashMap<Object, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) o).entrySet()) {
                Object serialize = serialize(plugin, entry.getValue(), field, key + "." + entry.getKey(), configuration);
                copy.put(entry.getKey(), serialize);
            }
            return copy;
        } else if (o instanceof List) {
            ArrayList<Object> copy = new ArrayList<>(((List) o).size());
            for (int i = 0; i < ((List) o).size(); i++) {
                copy.add(i, serialize(plugin, ((List) o).get(i), field, key, configuration));
            }
            return copy;
        }
        return o;
    }

    /**
     * 反序列化
     *
     * @param o
     * @return
     * @throws Exception
     */
    public static Object deSerialize(Object o) throws Exception {
        if (o instanceof MemorySection) {
            o = ((MemorySection) o).getValues(false);
        }
//        if(o instanceof ConfigurationSerializable)return o;
        if ((o instanceof Map && (((Map) o).containsKey("==") || ((Map) o).containsKey("!!")))) {
            String className = (String) ((Map) o).getOrDefault("==", ((Map) o).get("!!"));
            className = className.replace(VERSION_LABEL, CommonPlugin.packageName);
            Class<? extends ConfigurationSerializable> alias = null;
            try {
                // 用于兼容1.8
                alias = ConfigurationSerialization.getClassByAlias(className);
            } catch (NoSuchMethodError e) {
//                e.printStackTrace();
            }
            Class<?> clazz = null != alias ? alias : Class.forName(className);


            if (ConfigurationSerializable.class.isAssignableFrom(clazz)) {
                return deserialize((Class<? extends ConfigurationSerializable>) clazz, (Map) o);
            } else {
                Object object = clazz.newInstance();
                for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) o).entrySet()) {
                    if (entry.getKey().equals("==") || entry.getKey().equals("!!")) continue;
                    try {
                        Field field = getDeclareField(clazz, entry.getKey().toString());
                        if (field == null) continue;
                        Object value = entry.getValue();
                        if (value instanceof MemorySection)
                            value = ((MemorySection) value).getValues(true);
                        if (value instanceof LinkedHashMap)
                            value = deSerialize(value);
                        if (value instanceof List)
                            value = deSerialize(value);
                        if (field.getType().isEnum())
                            value = Enum.valueOf((Class<Enum>) field.getType(), (String) value);
                        value = fixClassTypeMismatch(className, field, value);
                        try {
                            field.setAccessible(true);
                            field.set(object, value);
                            field.setAccessible(false);
                        } catch (ClassCastException | IllegalArgumentException e) {
                            Bukkit.getServer().getLogger().warning(String.format("%s中的%s不是%s而是%s", className, field.getName(), value.getClass(), field.getType()));
                            throw e;
                        }
                    } catch (NoSuchFieldException e) {
                        Bukkit.getServer().getLogger().warning(String.format("%s中不存在名为%s的元素", className, entry.getKey()));
                    }
                }
                return object;
            }
        } else if (o instanceof Map) {
            Map map = new LinkedHashMap(((Map) o).size());
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) o).entrySet()) {
                map.put(entry.getKey(), deSerialize(entry.getValue()));
            }
            return map;
        } else if (o instanceof List) {
            List list = new ArrayList(((List) o).size());
            for (int i = 0; i < ((List) o).size(); i++) {
                list.add(i, deSerialize(((List) o).get(i)));
            }
            return list;
        }
        return o;

    }

    /**
     * 兼容手滑类型
     *
     * @param className
     * @param field
     * @param value
     * @return
     */
    public static Object fixClassTypeMismatch(String className, Field field, Object value) {
        if (value != null && Primitives.wrap(field.getType()) != Primitives.wrap(value.getClass())
                && (Primitives.isWrapperType(value.getClass()) || value.getClass() == String.class)
        ) {
            switch (Primitives.wrap(field.getType()).getSimpleName()) {
                case "Double":
                    Bukkit.getServer().getLogger().warning(String.format("%s中的%s不是%s而是%s", className, field.getName(), value.getClass().getSimpleName(), field.getType().getSimpleName()));
                    value = new BigDecimal(value.toString()).doubleValue();
                    break;
                case "Integer":
                    Bukkit.getServer().getLogger().warning(String.format("%s中的%s不是%s而是%s", className, field.getName(), value.getClass().getSimpleName(), field.getType().getSimpleName()));
                    value = new BigDecimal(value.toString()).intValue();
                    break;
                case "String":
                    Bukkit.getServer().getLogger().warning(String.format("%s中的%s不是%s而是%s", className, field.getName(), value.getClass().getSimpleName(), field.getType().getSimpleName()));
                    value = value.toString();
                    break;
                case "Float":
                    Bukkit.getServer().getLogger().warning(String.format("%s中的%s不是%s而是%s", className, field.getName(), value.getClass().getSimpleName(), field.getType().getSimpleName()));
                    value = new BigDecimal(value.toString()).floatValue();
                    break;
                default:
                    break;
            }

        }
        return value;
    }

    /**
     * 获取自己或父类的field
     *
     * @param clazz
     * @param key
     * @return
     * @throws NoSuchFieldException
     */
    public static Field getDeclareField(Class<?> clazz, String key) throws NoSuchFieldException {
        for (Class i = clazz; i != Object.class; i = i.getSuperclass()) {
            try {
                Field field = i.getDeclaredField(key);
                return field;
            } catch (NoSuchFieldException | SecurityException e) {
                continue;
            }
        }
        throw new NoSuchFieldException();
    }

    /**
     * 转为普通kv形式
     *
     * @param plugin
     * @param o
     * @param key
     * @param configuration
     * @return
     */
    public static Map<String, Object> toMap(Pluggable plugin, Object o, String key, CommentConfig configuration) {
        Class clazz = o.getClass();
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("==", clazz.getName() + "");
        MultiIterator<Field> classMultiIterator = getFieldsWithSuperClass(clazz);
        classMultiIterator.iterator(field -> {
            if (field.isAnnotationPresent(Ignore.class))
                return;
            String name = field.getName();
            if (name.contains("$")) return;
            field.setAccessible(true);
            Object value = field.get(o);
            field.setAccessible(false);
//            if (field.getType().isEnum()) {
//                value = ((Enum) value).name();
//            }
            String comment = null;
            if (field.isAnnotationPresent(Comment.class)) {
                Comment annotation = field.getAnnotation(Comment.class);
                comment = annotation.value();
            }
            String s = key + "." + name;
            configuration.set(s, comment);
            map.put(name, serialize(plugin, value, field, s, configuration));
        });


        return map;
    }

    public static MultiIterator<Field> getFieldsWithSuperClass(Class clazz) {
        return getFieldsWithSuperClass(clazz, new MultiIterator<>());
    }

    private static MultiIterator<Field> getFieldsWithSuperClass(Class clazz, MultiIterator<Field> iterator) {
        if (clazz == Object.class) return iterator;
        iterator = getFieldsWithSuperClass(clazz.getSuperclass(), iterator);
        iterator.add(Arrays.asList(clazz.getDeclaredFields()));
        return iterator;
    }

    public static ConfigurationSerializable deserialize(Class<? extends ConfigurationSerializable> clazz, Map<String, ?> args) throws Exception {
        args = recurciveDeserialize(args);

        Validate.notNull(args, "Args must not be null");


        ConfigurationSerializable result = null;
        Method method = null;

        if (result == null) {
//            method = getMethod(clazz, "deserialize", true);
            method = getDeserializeMethod(clazz);

            if (method != null) {
                result = deserializeViaMethod(clazz, method, args);
            }
        }

        if (result == null) {
            method = getMethod(clazz, "valueOf", true);

            if (method != null) {
                result = deserializeViaMethod(clazz, method, args);
            }
        }

        if (result == null) {
            Constructor<? extends ConfigurationSerializable> constructor = getConstructor(clazz);

            if (constructor != null) {
                result = deserializeViaCtor(clazz, constructor, args);
            }
        }

        return result;
    }

    private static Method getDeserializeMethod(Class<? extends ConfigurationSerializable> clazz) {
        DelegateDeserialization annotation = clazz.getAnnotation(DelegateDeserialization.class);
        Class<? extends ConfigurationSerializable> value;
        if (annotation == null || annotation.value() == null) {
            value = clazz;
        } else {
            value = annotation.value();
        }
        try {
            return value.getDeclaredMethod("deserialize", Map.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }


        return null;
    }

    private static Map<String, ?> recurciveDeserialize(Map<String, ?> args) throws Exception {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, ?> entry : args.entrySet()) {
            Object o = entry.getValue();
            try {
                o = deSerialize(entry.getValue());
            } catch (Exception ignored) {

            } finally {
                map.put(entry.getKey(), o);
            }

        }
        return map;
    }

    public static Method getMethod(Class<? extends ConfigurationSerializable> clazz, String name, boolean isStatic) {
        try {
            Method method = clazz.getDeclaredMethod(name, Map.class);

            if (!ConfigurationSerializable.class.isAssignableFrom(method.getReturnType())) {
                return null;
            }
            if (Modifier.isStatic(method.getModifiers()) != isStatic) {
                return null;
            }

            return method;
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (SecurityException ex) {
            return null;
        }
    }

    public static ConfigurationSerializable deserializeViaMethod(Class<? extends ConfigurationSerializable> clazz, Method method, Map<String, ?> args) {
        try {
            ConfigurationSerializable result = (ConfigurationSerializable) method.invoke(null, args);

            if (result == null) {
                Logger.getLogger(ConfigurationSerialization.class.getName()).log(Level.SEVERE, "Could not call method '" + method.toString() + "' of " + clazz + " for deserialization: method returned null");
            } else {
                return result;
            }
        } catch (Throwable ex) {
            Logger.getLogger(ConfigurationSerialization.class.getName()).log(
                    Level.SEVERE,
                    "Could not call method '" + method.toString() + "' of " + clazz + " for deserialization",
                    ex instanceof InvocationTargetException ? ex.getCause() : ex);
        }

        return null;
    }

    public static ConfigurationSerializable deserializeViaCtor(Class<? extends ConfigurationSerializable> clazz, Constructor<? extends ConfigurationSerializable> ctor, Map<String, ?> args) {
        try {
            return ctor.newInstance(args);
        } catch (Throwable ex) {
            Logger.getLogger(ConfigurationSerialization.class.getName()).log(
                    Level.SEVERE,
                    "Could not call constructor '" + ctor.toString() + "' of " + clazz + " for deserialization",
                    ex instanceof InvocationTargetException ? ex.getCause() : ex);
        }

        return null;
    }

    public static Constructor<? extends ConfigurationSerializable> getConstructor(Class<? extends ConfigurationSerializable> clazz) {
        try {
            return clazz.getConstructor(Map.class);
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (SecurityException ex) {
            return null;
        }
    }
}
