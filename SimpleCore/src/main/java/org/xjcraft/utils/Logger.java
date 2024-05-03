package org.xjcraft.utils;

import lombok.Data;
import org.xjcraft.adapter.LoggerApi;

@Data
public class Logger {

    public static LoggerApi logger;

    public static LoggerApi getLogger() {
        return logger;
    }

    public static void setLogger(LoggerApi logger) {
        Logger.logger = logger;
    }
}
