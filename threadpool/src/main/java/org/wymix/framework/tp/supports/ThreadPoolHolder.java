package org.wymix.framework.tp.supports;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadPoolHolder {

    private static final ConcurrentHashMap<String, ExecutorWrapper> THREAD_POOL_MAP = new ConcurrentHashMap<>();

    public static void put(String threadPoolName, ExecutorWrapper executorWrapper) {
        if (THREAD_POOL_MAP.containsKey(threadPoolName)) {
            throw new RuntimeException("ThreadPool[" + threadPoolName + "] was exists!");
        }
        THREAD_POOL_MAP.put(threadPoolName, executorWrapper);
    }

    public static ExecutorWrapper get(String threadPoolName) {
        return THREAD_POOL_MAP.get(threadPoolName);
    }

    public static Set<String> threadPoolNames() {
        return THREAD_POOL_MAP.keySet();
    }

}
