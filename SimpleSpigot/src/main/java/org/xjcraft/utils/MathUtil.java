package org.xjcraft.utils;

public class MathUtil {
    public static int random(int min, int max) {
        return (int) Math.floor(Math.random() * (max - min + 1) + min);
    }

    public static int random(int max) {
        return random(0, max);
    }


}
