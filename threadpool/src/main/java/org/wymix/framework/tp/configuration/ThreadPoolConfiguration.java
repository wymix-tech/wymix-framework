package org.wymix.framework.tp.configuration;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.wymix.framework.tp.properties.ThreadPoolProperties;
import org.wymix.framework.tp.supports.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@AutoConfiguration(
        before = {TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class})
@ConditionalOnProperty(
        prefix = "wymix.threadpool",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ThreadPoolConfiguration {
    @Bean
    static ExecutorRegistrar executorRegistrar() {
        return new ExecutorRegistrar();
    }

    @Bean
    static NativeThreadPoolMonitor nativeThreadPoolMonitor() {
        return new NativeThreadPoolMonitor();
    }

    static final class ExecutorRegistrar
            implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
        private static final Set<String> RESERVED =
                Set.of(
                        "applicationTaskExecutor",
                        "taskExecutor",
                        "taskScheduler",
                        "bootstrapExecutor");
        private Environment environment;

        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
            ThreadPoolProperties properties =
                    Binder.get(environment).bindOrCreate("thread-pool", ThreadPoolProperties.class);
            String propertyBean = ThreadPoolProperties.class.getName();
            if (!registry.containsBeanDefinition(propertyBean)) {
                registry.registerBeanDefinition(
                        propertyBean,
                        new RootBeanDefinition(ThreadPoolProperties.class, () -> properties));
            }
            register(registry, properties.getPool(), false);
            register(registry, properties.getScheduled(), true);
        }

        private void register(
                BeanDefinitionRegistry registry,
                Map<String, ThreadPoolProperties.CoreParams> pools,
                boolean scheduled) {
            if (pools == null) return;
            pools.forEach(
                    (name, params) -> {
                        if (RESERVED.contains(name)
                                || registry.containsBeanDefinition(name)
                                || registry.isAlias(name)) {
                            throw new IllegalArgumentException(
                                    "Thread pool name is reserved or already registered: " + name);
                        }
                        if (params.getCorePoolSize() <= 0
                                || (!scheduled
                                        && (params.getMaxPoolSize() < params.getCorePoolSize()
                                                || params.getQueueCapacity() < 0
                                                || params.getKeepAliveSeconds() < 0))) {
                            throw new IllegalArgumentException(
                                    "Invalid configuration for thread pool: " + name);
                        }
                        RejectedExecutionHandler handler = rejectionHandler(name, params);
                        RootBeanDefinition definition =
                                scheduled
                                        ? new RootBeanDefinition(
                                                ScheduledExecutorServiceWrapper.class,
                                                () ->
                                                        (ScheduledExecutorServiceWrapper)
                                                                ThreadPoolRegistry
                                                                        .registerScheduledThreadPool(
                                                                                name,
                                                                                params
                                                                                        .getCorePoolSize(),
                                                                                handler))
                                        : new RootBeanDefinition(
                                                ExecutorServiceWrapper.class,
                                                () ->
                                                        (ExecutorServiceWrapper)
                                                                ThreadPoolRegistry
                                                                        .registerThreadPool(
                                                                                name,
                                                                                params
                                                                                        .getCorePoolSize(),
                                                                                params
                                                                                        .getMaxPoolSize(),
                                                                                params
                                                                                        .getKeepAliveSeconds(),
                                                                                TimeUnit.SECONDS,
                                                                                params
                                                                                                        .getQueueCapacity()
                                                                                                == 0
                                                                                        ? new SynchronousQueue<>()
                                                                                        : new LinkedBlockingQueue<>(
                                                                                                params
                                                                                                        .getQueueCapacity()),
                                                                                handler));
                        // Business pools never suppress Boot's applicationTaskExecutor.
                        definition.setDefaultCandidate(false);
                        definition.setDestroyMethodName("destroy");
                        registry.registerBeanDefinition(name, definition);
                    });
        }

        private RejectedExecutionHandler rejectionHandler(
                String name, ThreadPoolProperties.CoreParams params) {
            try {
                return Class.forName(params.getRejectedPolicyClassName())
                        .asSubclass(RejectedExecutionHandler.class)
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (ReflectiveOperationException | RuntimeException ex) {
                throw new IllegalArgumentException(
                        "Invalid rejection handler for thread pool: " + name, ex);
            }
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {}
    }
}
