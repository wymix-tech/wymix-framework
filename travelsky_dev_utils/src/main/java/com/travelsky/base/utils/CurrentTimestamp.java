package com.travelsky.base.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public enum CurrentTimestamp {
    SECOND(1_000),
    MILLISSECOND(1),
    ;

    private long unit;

    CurrentTimestamp(long unit) {
        this.unit = unit;
    }

    public Long getTimestamp() {
        return SystemClockHolder.INSTANCE.getTime() / unit;
    }

    private static final class SystemClock {
        private final LongAdder CURRENT_TIME = new LongAdder();

        SystemClock() {
            CURRENT_TIME.add(System.currentTimeMillis());
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("System-Clock-Thread");
                thread.setDaemon(true);
                return thread;
            }).scheduleAtFixedRate(CURRENT_TIME::increment, 1, 1, TimeUnit.MILLISECONDS);
        }

        long getTime() {
            return CURRENT_TIME.longValue();
        }
    }

    private static final class SystemClockHolder {
        static final SystemClock INSTANCE = new SystemClock();
    }

}
