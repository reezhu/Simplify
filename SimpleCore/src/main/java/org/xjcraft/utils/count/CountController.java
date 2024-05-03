package org.xjcraft.utils.count;

import lombok.Getter;
import lombok.Setter;

import java.util.Timer;
import java.util.TimerTask;

public class CountController {
    @Getter
    @Setter
    protected int number;
    protected CountCallback countCallback;
    protected Timer timer = new Timer();
    @Getter
    boolean running = true;

    public CountController(int n, CountCallback countCallback) {
        this.number = n;
        this.countCallback = countCallback;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (number > 0) {
                    countCallback.onCount(number);
                    number--;
                } else {
                    countCallback.onFinish();
                    timer.cancel();
                    running = false;
                }

            }
        }, 0, 1000);
    }

    public void stop() {
        timer.cancel();
        running = false;
        countCallback.onStop();
    }
}
