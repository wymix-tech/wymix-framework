package com.travelsky.base.core.listener;

import com.travelsky.base.utils.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.MDC;
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
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.File;
import java.util.Properties;

public class LogApplicationStartedEventListener implements GenericApplicationListener {

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
            String logHome;
            String logFileName;
            Properties source = new Properties();
            PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource("framework-log-custom", source);
            MutablePropertySources propertySources = environment.getPropertySources();


            if (!environment.containsProperty("logging.file") || StringUtils.isBlank(environment.getProperty("logging.file"))) {
                source.put("logging.file", File.separator + DEFAULT_LOG_HOME + File.separator + environment.getProperty("spring.application.name", DEFAULT_LOG_FILE_NAME) + ".log");
                environment.getPropertySources().addLast(propertiesPropertySource);
                logHome = File.separator + DEFAULT_LOG_HOME;
                logFileName = environment.getProperty("spring.application.name", DEFAULT_LOG_FILE_NAME);
            } else {
                String property = environment.getProperty("logging.file");
                logFileName = FilenameUtils.getBaseName(property);
                logHome = FilenameUtils.getPath(property);
            }
            MDC.put("logHome", logHome);
            MDC.put("logFilename", logFileName);

            if (!environment.containsProperty("logging.pattern.file") || StringUtils.isBlank(environment.getProperty("logging.pattern.file"))) {
                source.put("logging.pattern.file", "%d{yyyy-mm-dd HH:mm:ss.SSS} [%t][%-5level] %-36.36c{1.}(:%-5L) -[%X{traceId:-defaultTraceId}] - %msg%n");
            }
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
