package org.xjcraft.adapter;

import java.util.logging.Level;

public interface LoggerApi {
    void debug(String msg);

    void warning(String msg);

    void info(String msg);

    void error(String msg);

    default void log(Level level, String msg) {
        if (level.intValue() >= 1000) {
            error(msg);
        } else if (level.intValue() >= 900) {
            warning(msg);
        } else if (level.intValue() >= 800) {
            info(msg);
        } else if (level.intValue() >= 0) {
            debug(msg);
        }
    }

    ;
}
