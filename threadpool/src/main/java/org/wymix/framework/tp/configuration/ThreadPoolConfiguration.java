package org.wymix.framework.tp.configuration;

import org.wymix.framework.tp.properties.ThreadPoolProperties;
import org.wymix.framework.tp.supports.ExecutorWrapper;
import org.wymix.framework.tp.supports.ThreadPoolRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@AutoConfiguration
@ConditionalOnProperty(prefix = "wymix.threadpool", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class ThreadPoolConfiguration {

    private ThreadPoolProperties threadPoolProperties;

    @Bean
    ExecutorRegistrar executorRegistrar() {
        return new ExecutorRegistrar();
    }


    class ExecutorRegistrar implements BeanDefinitionRegistryPostProcessor, org.springframework.beans.factory.DisposableBean {
        private final java.util.Map<String, ExecutorWrapper> managed = new java.util.HashMap<>();
        @Override public void destroy() {
            managed.forEach((name, executor) -> { executor.shutdown(); org.wymix.framework.tp.supports.ThreadPoolHolder.unregister(name, executor); });
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            ConfigurableEnvironment environment = beanFactory.getBean(ConfigurableEnvironment.class);
            Binder binder = new Binder(environment.getPropertySources().stream().map(ConfigurationPropertySource::from).filter(e -> e != null).toList());

            threadPoolProperties = binder.bindOrCreate("thread-pool", ThreadPoolProperties.class);
            beanFactory.registerSingleton(ThreadPoolProperties.class.getName(), threadPoolProperties);
            beanFactory.autowireBean(threadPoolProperties);
            beanFactory.initializeBean(threadPoolProperties, ThreadPoolProperties.class.getName());

            Map<String, ThreadPoolProperties.CoreParams> pool = threadPoolProperties.getPool();
            ExecutorWrapper executorWrapper;
            if (pool != null && !pool.isEmpty()) {
                for (Map.Entry<String, ThreadPoolProperties.CoreParams> entry : pool.entrySet()) {
                    try {
                        executorWrapper = ThreadPoolRegistry.registerThreadPool(entry.getKey(), entry.getValue().getCorePoolSize(), entry.getValue().getMaxPoolSize(), entry.getValue().getKeepAliveSeconds(), TimeUnit.SECONDS, new LinkedBlockingQueue<>(entry.getValue().getQueueCapacity()), (RejectedExecutionHandler) Class.forName(entry.getValue().getRejectedPolicyClassName()).getDeclaredConstructor().newInstance());
                        beanFactory.registerSingleton(entry.getKey(), executorWrapper);
                    managed.put(entry.getKey(), executorWrapper);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

            }
            Map<String, ThreadPoolProperties.CoreParams> scheduled = threadPoolProperties.getScheduled();
            if (scheduled != null && !scheduled.isEmpty()) {
                for (Map.Entry<String, ThreadPoolProperties.CoreParams> entry : scheduled.entrySet()) {
                    try {
                        executorWrapper = ThreadPoolRegistry.registerScheduledThreadPool(entry.getKey(), entry.getValue().getCorePoolSize(), (RejectedExecutionHandler) Class.forName(entry.getValue().getRejectedPolicyClassName()).getDeclaredConstructor().newInstance());
                        beanFactory.registerSingleton(entry.getKey(), executorWrapper);
                    managed.put(entry.getKey(), executorWrapper);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        }
    }


}
