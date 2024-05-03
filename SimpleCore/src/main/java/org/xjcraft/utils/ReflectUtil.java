package org.xjcraft.utils;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.Annotation;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ReflectUtil {

    public static Object getHandle(Object bukkitObj) {
        try {
            return bukkitObj.getClass().getMethod("getHandle").invoke(bukkitObj);
        } catch (Exception e) {
        }
        return null;
    }

    public static Field getFieldByName(Class source, String name) {
        try {
            Field field = source.getField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Field getDeclaredFieldByName(Class source, String name) {
        try {
            Field field = source.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Field> getFieldByType(Class source, Class type) {
        List<Field> list = new ArrayList<>();
        for (Field field : source.getFields()) {
            if (field.getType() == type) {
                field.setAccessible(true);
                list.add(field);
            }
        }
        return list;
    }

    public static List<Field> getDeclaredFieldByType(Class source, Class type) {
        List<Field> list = new ArrayList<>();
        for (Field field : source.getDeclaredFields()) {
            if (field.getType() == type) {
                field.setAccessible(true);
                list.add(field);
            }
        }
        return list;
    }

    public static Method getMethodByNameAndParams(Class source, String name, Class... args) {
        for (Method method : findMethodByParams(source.getMethods(), args)) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    public static Method getDeclaredMethodByNameAndParams(Class source, String name, Class... args) {
        for (Method method : findMethodByParams(source.getDeclaredMethods(), args)) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    public static List<Method> getMethodByParams(Class source, Class... args) {
        return findMethodByParams(source.getMethods(), args);
    }

    public static List<Method> getDeclaredMethodByParams(Class source, Class... args) {
        return findMethodByParams(source.getDeclaredMethods(), args);
    }

    public static List<Method> getMethodByParamsAndType(Class source, Class returnType, Class... args) {
        List<Method> methods = new ArrayList<>();
        for (Method method : findMethodByParams(source.getMethods(), args)) {
            if (method.getReturnType().equals(returnType)) {
                methods.add(method);
            }
        }
        return methods;
    }

    public static List<Method> getDeclaredMethodByParamsAndType(Class source, Class returnType, Class... args) {
        List<Method> methods = new ArrayList<>();
        for (Method method : findMethodByParams(source.getDeclaredMethods(), args)) {
            if (method.getReturnType().equals(returnType)) {
                methods.add(method);
            }
        }
        return methods;
    }

    public static List<Method> getMethodByNameAndType(Class source, String name, Class returnType) {
        List<Method> methods = new ArrayList<>();
        for (Method method : source.getMethods()) {
            if (method.getName().equals(name) && method.getReturnType().equals(returnType)) {
                methods.add(method);
            }
        }
        return methods;
    }

    public static List<Method> getDeclaredMethodByNameAndType(Class source, String name, Class returnType) {
        List<Method> methods = new ArrayList<>();
        for (Method method : source.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getReturnType().equals(returnType)) {
                methods.add(method);
            }
        }
        return methods;
    }

    public static List<Method> getMethodByType(Class source, Class returnType) {
        List<Method> methods = new ArrayList<>();
        for (Method method : source.getMethods()) {
            if (method.getReturnType().equals(returnType)) {
                methods.add(method);
            }
        }
        return methods;
    }

    public static List<Method> getDeclaredMethodByType(Class source, Class returnType) {
        List<Method> methods = new ArrayList<>();
        for (Method method : source.getDeclaredMethods()) {
            if (method.getReturnType().equals(returnType)) {
                methods.add(method);
            }
        }
        return methods;
    }

    private static List<Method> findMethodByParams(Method[] methods, Class... args) {
        List<Method> list = new ArrayList<>();
        start:
        for (Method method : methods) {
            if (method.getParameterTypes().length == args.length) {
                Class[] array = method.getParameterTypes();
                for (int i = 0; i < args.length; i++) {
                    if (array[i] != args[i]) {
                        continue start;
                    }
                }
                method.setAccessible(true);
                list.add(method);
            }
        }
        return list;
    }

    public static Method getDeclaredMethod(Class clzz, String methodName, Class... args) {
        try {
            return clzz.getDeclaredMethod(methodName, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void invokeMethod(Object object, String methodName, Class arg, Object value) {
        try {
            Method m = object.getClass().getDeclaredMethod(methodName, arg);
            m.invoke(object, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void invokeMethod(Object object, String methodName, Object value) {
        try {
            Method m = object.getClass().getDeclaredMethod(methodName, value.getClass());
            m.invoke(object, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void invokeMethod(Object object, String methodName, Class[] args, Object[] value) {
        try {
            Method m = object.getClass().getDeclaredMethod(methodName, args);
            m.invoke(object, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public static Set<Class<?>> getAllClassesFromJarFile(Plugin plugin) {
//        return getAllClassesFromJarFile(plugin.getDataFolder().getParentFile().getAbsolutePath() + File.separator + plugin.getName() + ".jar");
//    }

    public static Set<Class<?>> getAllClassesFromJarFile(String jarPath) {
        Set<Class<?>> classes = new LinkedHashSet<>();//所有的Class对象  
        Map<Class<?>, Annotation[]> classAnnotationMap = new HashMap<>();//每个Class对象上的注释对象  
        Map<Class<?>, Map<Method, Annotation[]>> classMethodAnnoMap = new HashMap<>();//每个Class对象中每个方法上的注释对象  
        try {
            JarFile jarFile = new JarFile(new File(jarPath));
            URL url = new URL("file:" + jarPath);
            ClassLoader loader = new URLClassLoader(new URL[]{url});//自己定义的classLoader类，把外部路径也加到load路径里，使系统去该路经load对象  
            Enumeration<JarEntry> es = jarFile.entries();
            while (es.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) es.nextElement();
                String name = jarEntry.getName();
                if (name != null && name.endsWith(".class")) {//只解析了.class文件，没有解析里面的jar包  
                    //默认去系统已经定义的路径查找对象，针对外部jar包不能用  
                    //Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(name.replace("/", ".").substring(0,name.length() - 6));  
                    Class<?> c = loader.loadClass(name.replace("/", ".").substring(0, name.length() - 6));//自己定义的loader路径可以找到  
                    //输出class名称
                    //System.out.println(c);
                    classes.add(c);
                    //Annotation[] classAnnos = c.getDeclaredAnnotations();
                    //classAnnotationMap.put(c, classAnnos);
                    //Method[] classMethods = c.getDeclaredMethods();
                    //Map<Method, Annotation[]> methodAnnoMap = new HashMap<>();
                    //for (Method classMethod : classMethods) {
                    //    Annotation[] a = classMethod.getDeclaredAnnotations();
                    //    methodAnnoMap.put(classMethod, a);
                    //}
                    //classMethodAnnoMap.put(c, methodAnnoMap);
                }
            }
            //输出class数量
            //System.out.println(classes.size());
        } catch (ClassNotFoundException ex) {
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return classes;
    }

    public static Set<Class<?>> getAllClassesFromPackage(String path, File file, ClassLoader loader, String packet) {
        Set<Class<?>> classes = new LinkedHashSet<>();//所有的Class对象
        packet = packet.replace(".", "/");
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(file);
            URL url = new URL("file:" + file.getPath());
            Enumeration<JarEntry> es = jarFile.entries();
            while (es.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) es.nextElement();
                String name = jarEntry.getName();
                if (name.contains(packet))
//                System.out.println(name + "|||||" + jarPath+"|||"+name.contains(jarPath));
                    if (name.endsWith(".class") && name.contains(path)) {
                        //默认去系统已经定义的路径查找对象，针对外部jar包不能用
                        Class<?> c = loader.loadClass(name.replace("/", ".").substring(0, name.length() - 6));
                        //输出class名称
                        classes.add(c);
                    }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return classes;
    }


    public static void setFinalField(Field field, boolean setFinal) throws Exception {
        if (Modifier.isFinal(field.getModifiers())) {
            if (!setFinal) {
                final Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            }
        } else {
            if (setFinal) {
                final Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() | Modifier.FINAL);
            }
        }
    }

    public static void setField(Field field, Object object, Object value) throws Exception {
        boolean isAccessible = field.isAccessible();
        field.setAccessible(true);
        if (Modifier.isFinal(field.getModifiers())) {
            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(object, value);
            modifiersField.setInt(field, field.getModifiers() | Modifier.FINAL);
            modifiersField.setAccessible(false);
        } else {
            field.set(object, value);
        }
        if (!isAccessible) {
            field.setAccessible(false);
        }
    }
}