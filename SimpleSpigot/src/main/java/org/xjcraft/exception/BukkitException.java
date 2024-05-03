package org.xjcraft.exception;

import lombok.Getter;

/**
 * Created by Ree on 2017/8/15.
 */
public class BukkitException extends Exception {
    @Getter
    String msg;

    public BukkitException(String msg) {
        this.msg = msg;
    }

    @Override
    public void printStackTrace() {
        System.out.println(msg);
        super.printStackTrace();
    }
}
