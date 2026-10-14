package org.wymix.framework.tp.supports;

import java.util.concurrent.TimeUnit;

public class ThreadPoolOperator {

    public static void changeCorePoolSize(String threadPoolName, int corePoolSize) {
        ThreadPoolHolder.get(threadPoolName).setCorePoolSize(corePoolSize);
    }

    public static void changeMaxPoolSize(String threadPoolName, int maxPoolSize) {
        ThreadPoolHolder.get(threadPoolName).setMaxPoolSize(maxPoolSize);
    }

    public static void changeKeepAliveTimes(String threadPoolName, long keepAliveTimes, TimeUnit timeUnit) {
        ThreadPoolHolder.get(threadPoolName).setKeepAliveTime(keepAliveTimes, timeUnit);
    }

}
