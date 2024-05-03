package org.xjcraft.utils;

import com.oracle.truffle.js.scriptengine.GraalJSEngineFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Map;

public class ScriptUtil {
    private static final ScriptEngine engine = new GraalJSEngineFactory().getScriptEngine();


    public static String parse(String formulate, Map<String, String> placeholder) {
        try {
            return engine.eval(StringUtil.applyPlaceHolder(formulate, placeholder)).toString();
        } catch (ScriptException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ScriptEngine getEngine() {
        return engine;
    }
}
