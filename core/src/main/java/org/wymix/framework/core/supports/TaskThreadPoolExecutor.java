package org.wymix.framework.core.supports;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.wymix.framework.tp.supports.NativeThreadPoolAdapter;
import org.wymix.framework.tp.supports.ThreadPoolHolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * Compatibility entry point. New applications should use Boot's native executor and its
 * Builder/Customizer. All execution and lifecycle behavior is inherited from Spring; monitoring
 * does not replace the native executor.
 */
@Deprecated(since = "3.0.0", forRemoval = false)
public class TaskThreadPoolExecutor extends ThreadPoolTaskExecutor {
    private NativeThreadPoolAdapter monitor;
    private String registrationName;

    @Override
    protected ExecutorService initializeExecutor(
            ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        ExecutorService executor =
                super.initializeExecutor(threadFactory, rejectedExecutionHandler);
        registrationName = getThreadNamePrefix();
        monitor = new NativeThreadPoolAdapter(this, getThreadPoolExecutor());
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
