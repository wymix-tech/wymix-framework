package org.wymix.framework.tp.supports;

import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Observes native Spring pools after initialization without replacing any Bean. */
public final class NativeThreadPoolMonitor implements DestructionAwareBeanPostProcessor {
    private final Map<String, NativeThreadPoolAdapter> managed = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        NativeThreadPoolAdapter adapter = null;
        if (bean instanceof ThreadPoolTaskExecutor executor) {
            adapter = new NativeThreadPoolAdapter(executor, executor.getThreadPoolExecutor());
        } else if (bean instanceof ThreadPoolTaskScheduler scheduler) {
            // Custom schedulers may deliberately provide a non-native executor.
            if (scheduler.getScheduledExecutor()
                    instanceof java.util.concurrent.ScheduledThreadPoolExecutor pool) {
                adapter = new NativeThreadPoolAdapter(scheduler, pool);
            }
        }
        if (adapter != null) {
            ExecutorWrapper existing = ThreadPoolHolder.get(beanName);
            if (existing instanceof NativeThreadPoolAdapter registered
                    && ((bean instanceof ThreadPoolTaskExecutor executor
                                    && registered.observes(executor.getThreadPoolExecutor()))
                            || (bean instanceof ThreadPoolTaskScheduler scheduler
                                    && registered.observes(
                                            scheduler.getScheduledThreadPoolExecutor())))) {
                return bean;
            }
            ThreadPoolHolder.put(beanName, adapter);
            managed.put(beanName, adapter);
        }
        return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) {
        NativeThreadPoolAdapter adapter = managed.remove(beanName);
        if (adapter != null) ThreadPoolHolder.unregister(beanName, adapter);
    }

    @Override
    public boolean requiresDestruction(Object bean) {
        return bean instanceof ThreadPoolTaskExecutor || bean instanceof ThreadPoolTaskScheduler;
    }
}
