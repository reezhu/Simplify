package org.xjcraft.adapter;

import java.util.List;

public interface Pluggable {
    LoggerApi getLoggerApi();

    String getPackageName();

    List<Class<?>> getClasses(String packet);
}
