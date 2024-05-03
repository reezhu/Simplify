package org.xjcraft.utils;

import com.google.gson.Gson;

public class JSON {
    public static String toJSONString(Object o) {
        Gson gson = new Gson();
        return gson.toJson(o);
    }

    public static <T> T parseJSON(String text, Class<T> clazz) {
        Gson gson = new Gson();
        return gson.fromJson(text, clazz);
    }
}
