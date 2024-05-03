package org.xjcraft.manager;

import lombok.Data;
import org.xjcraft.annotation.RCommand;
import org.xjcraft.api.CommonCommandExecutor;

import java.lang.reflect.Method;

/**
 * @author Ree
 */
@Data
public class CommandElement {
    Class[] parameters;
    Integer size;
    Method method;
    String example;
    CommonCommandExecutor excutor;
    RCommand config;
}
