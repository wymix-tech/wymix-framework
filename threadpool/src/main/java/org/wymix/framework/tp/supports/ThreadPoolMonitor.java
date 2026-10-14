package org.wymix.framework.tp.supports;

import java.util.Arrays;
import java.util.List;

public class ThreadPoolMonitor {

    public static String[] getAllThreadPoolNames() {
        return ThreadPoolHolder.threadPoolNames().stream().toArray(String[]::new);
    }

    public static List<ThreadPoolContext> allThreadPoolContext() {
        return Arrays.stream(getAllThreadPoolNames())
                .map(ThreadPoolMonitor::getThreadPoolContext)
                .toList();
    }

    public static ThreadPoolContext getThreadPoolContext(String threadPoolName) {
        ExecutorWrapper executorWrapper = ThreadPoolHolder.get(threadPoolName);
        if (executorWrapper == null)
            throw new IllegalArgumentException("Unknown thread pool: " + threadPoolName);
        ThreadPoolContext context =
                new ThreadPoolContext(
                        threadPoolName,
                        executorWrapper.getCorePoolSize(),
                        executorWrapper.getMaxPoolSize(),
                        executorWrapper.getKeepAliveTime(),
                        executorWrapper.getTimeUnit(),
                        executorWrapper.standby(),
                        executorWrapper.running(),
                        executorWrapper.remaining(),
                        executorWrapper.total(),
                        executorWrapper.finished(),
                        executorWrapper.successRate(),
                        executorWrapper.executionRate(),
                        executorWrapper.processedRate(),
                        executorWrapper.cost(),
                        executorWrapper.avgCost());
        context.setPoolSize(executorWrapper.getPoolSize());
        context.setOutcomeMetricsAvailable(executorWrapper.outcomeMetricsAvailable());
        return context;
    }
}
