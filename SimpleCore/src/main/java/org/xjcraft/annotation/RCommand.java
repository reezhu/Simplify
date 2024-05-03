package org.xjcraft.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Ree on 2017/8/7.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RCommand {
    String value();

    String permisson() default "";

    Sender sender() default Sender.ALL;

    Permisson defaultUser() default Permisson.ALL;

    String desc() default "";

    enum Sender {
        ALL, PLAYER, CONSOLE
    }

    enum Permisson {
        OP, ALL, NONE, NotOP
    }


}



