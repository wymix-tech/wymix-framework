package org.wymix.framework.core.supports;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.wymix.framework.tp.supports.NativeThreadPoolAdapter;
import org.wymix.framework.tp.supports.ThreadPoolHolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;

/** Compatibility entry point retaining Spring's native scheduler implementation. */
@Deprecated(since = "3.0.0", forRemoval = false)
public class ScheduledTaskThreadPoolExecutor extends ThreadPoolTaskScheduler {
    private String name;
    private String registrationName;
    private NativeThreadPoolAdapter monitor;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected ExecutorService initializeExecutor(
            ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        ExecutorService executor =
                super.initializeExecutor(threadFactory, rejectedExecutionHandler);
        registrationName = name == null ? getThreadNamePrefix() : name;
        monitor = new NativeThreadPoolAdapter(this, getScheduledThreadPoolExecutor());
        ThreadPoolHolder.put(registrationName, monitor);
        return executor;
    }

    @Override
    public void destroy() {
        try {
            super.destroy();
        } finally {
            if (monitor != null) ThreadPoolHolder.unregister(registrationName, monitor);
        }
    }
}
