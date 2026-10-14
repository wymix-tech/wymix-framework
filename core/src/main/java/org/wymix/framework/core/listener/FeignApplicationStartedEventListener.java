package org.wymix.framework.core.listener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FeignApplicationStartedEventListener implements GenericApplicationListener {

    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
    private static final String DEFAULT_LOG_HOME = "logs";
    private static final String DEFAULT_LOG_FILE_NAME = "app";
    private static Class<?>[] EVENT_TYPES = {ApplicationStartingEvent.class, ApplicationEnvironmentPreparedEvent.class,
            ApplicationPreparedEvent.class, ContextClosedEvent.class, ApplicationFailedEvent.class};
    private static Class<?>[] SOURCE_TYPES = {SpringApplication.class, ApplicationContext.class};

    static {
        System.setProperty("logging.log4j2.config.override", "classpath:log4j2-default.xml");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            ConfigurableEnvironment environment = ((ApplicationEnvironmentPreparedEvent) event).getEnvironment();

            Properties source = new Properties();
            PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource("framework-feign-custom", source);
            MutablePropertySources propertySources = environment.getPropertySources();
            String prefix = "http.requests";
            Map<String, Object> properties = new HashMap<>();
            for (PropertySource<?> propertySource : propertySources) {
                if (propertySource instanceof EnumerablePropertySource) {
                    EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>) propertySource;
                    for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                        if (propertyName.startsWith(prefix)) {
                            String key = "spring.cloud.loadbalancer" + propertyName.substring(prefix.length());
                            if (propertyName.endsWith(".retry.enabled")) {
                                if (!environment.containsProperty("spring.cloud.loadbalancer.retry.enabled")) {
                                    properties.put(key, enumerablePropertySource.getProperty(propertyName));
                                }
                            } else if (propertyName.endsWith(".retry.retryOnAllOperations")) {
                                if (!environment.containsProperty("spring.cloud.loadbalancer.retry.retryOnAllOperations")) {
                                    properties.put(key, enumerablePropertySource.getProperty(propertyName));
                                }
                            } else if (propertyName.endsWith(".retry.maxRetriesOnSameServiceInstance")) {
                                if (!environment.containsProperty("spring.cloud.loadbalancer.retry.maxRetriesOnSameServiceInstance")) {
                                    properties.put(key, enumerablePropertySource.getProperty(propertyName));
                                }
                            } else if (propertyName.endsWith(".retry.maxRetriesOnNextServiceInstance")) {
                                if (!environment.containsProperty("spring.cloud.loadbalancer.retry.maxRetriesOnNextServiceInstance")) {
                                    properties.put(key, enumerablePropertySource.getProperty(propertyName));
                                }
                            } else if (propertyName.endsWith(".retry.backoff.enabled")) {
                                if (!environment.containsProperty("spring.cloud.loadbalancer.retry.backoff.enabled")) {
                                    properties.put(key, enumerablePropertySource.getProperty(propertyName));
                                }
                            } else if (propertyName.endsWith(".retry.backoff.minBackoff")) {
                                if (!environment.containsProperty("spring.cloud.loadbalancer.retry.backoff.minBackoff")) {
                                    properties.put(key, enumerablePropertySource.getProperty(propertyName));
                                }
                            } else if (propertyName.endsWith(".retry.backoff.maxBackoff")) {
                                if (!environment.containsProperty("spring.cloud.loadbalancer.retry.backoff.maxBackoff")) {
                                    properties.put(key, enumerablePropertySource.getProperty(propertyName));
                                }
                            } else if (propertyName.endsWith(".retry.backoff.jitter")) {
                                if (!environment.containsProperty("spring.cloud.loadbalancer.retry.backoff.jitter")) {
                                    properties.put(key, enumerablePropertySource.getProperty(propertyName));
                                }
                            } else {
                                properties.put("spring.cloud.discovery.client.simple.instances" + propertyName.substring(prefix.length()), enumerablePropertySource.getProperty(propertyName));
                            }
                        }
                    }
                }
            }
            source.putAll(properties);
            propertySources.addLast(propertiesPropertySource);
        }
    }

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }

    @Override
    public boolean supportsEventType(ResolvableType resolvableType) {
        return isAssignableFrom(resolvableType.getRawClass(), EVENT_TYPES);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return isAssignableFrom(sourceType, SOURCE_TYPES);
    }

    private boolean isAssignableFrom(Class<?> type, Class<?>... supportedTypes) {
        if (type != null) {
            for (Class<?> supportedType : supportedTypes) {
                if (supportedType.isAssignableFrom(type)) {
                    return true;
                }
            }
        }
        return false;
    }

}
