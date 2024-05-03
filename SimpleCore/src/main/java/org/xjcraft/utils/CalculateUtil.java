package org.xjcraft.utils;



import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.logging.Level;

public class CalculateUtil {
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

    public static String getResult(String formulate) {
        return getResult(formulate, null);
    }

    public static String getResult(String formulate, HashMap<String, String> keys) {
        if (keys != null && keys.size() > 0) {
            formulate = StringUtil.applyPlaceHolder(formulate, keys);
        }
        try {
            return engine.eval(formulate).toString();
        } catch (ScriptException e) {
            Logger.getLogger().log(Level.WARNING, "无法解析公式：" + formulate);
            throw new IllegalArgumentException("无法解析公式：" + formulate);
        }
    }

    public static <I> I getInterface(Class<I> c, String script) {
        ScriptEngine engine = new ScriptEngineManager(String.class.getClassLoader()).getEngineByExtension("js");
        try {
            engine.eval(script);
            return ((Invocable) engine).getInterface(c);
        } catch (ScriptException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
