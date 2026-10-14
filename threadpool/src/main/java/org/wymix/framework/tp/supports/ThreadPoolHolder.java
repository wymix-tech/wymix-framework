package org.wymix.framework.tp.supports;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadPoolHolder {

    private static final ConcurrentHashMap<String, ExecutorWrapper> THREAD_POOL_MAP = new ConcurrentHashMap<>();

    public static void put(String threadPoolName, ExecutorWrapper executorWrapper) {
        if (THREAD_POOL_MAP.putIfAbsent(threadPoolName, executorWrapper) != null) {
            executorWrapper.shutdownNow();
            throw new IllegalArgumentException("ThreadPool[" + threadPoolName + "] already exists");
        }
    }

    public static void unregister(String name, ExecutorWrapper expected) {
        THREAD_POOL_MAP.remove(name, expected);
    }

    public static ExecutorWrapper get(String threadPoolName) {
        return THREAD_POOL_MAP.get(threadPoolName);
    }

    public static Set<String> threadPoolNames() {
        return THREAD_POOL_MAP.keySet();
    }

}
