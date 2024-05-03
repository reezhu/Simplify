package org.xjcraft.utils;

import java.util.HashMap;

public class CompatibleHashMap<V> extends HashMap<String, V> {

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        if ((key instanceof String) && ((String) key).endsWith(".yml"))
            key = ((String) key).substring(0, ((String) key).length() - 4);
        return super.getOrDefault(key, defaultValue);
    }

    @Override
    public V get(Object key) {
        if ((key instanceof String) && ((String) key).endsWith(".yml"))
            key = ((String) key).substring(0, ((String) key).length() - 4);
        return super.get(key);
    }

    @Override
    public V put(String key, V value) {
        if ((key != null) && key.endsWith(".yml"))
            key = key.substring(0, key.length() - 4);
        return super.put(key, value);
    }

    @Override
    public V putIfAbsent(String key, V value) {
        if ((key != null) && key.endsWith(".yml"))
            key = key.substring(0, key.length() - 4);
        return super.putIfAbsent(key, value);
    }
}
