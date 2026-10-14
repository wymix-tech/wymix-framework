package org.wymix.framework.core.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static final ApplicationContext getApplicationContext() {
        return ApplicationContextHolder.applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.applicationContext = applicationContext;

    }

    public static final <T> T getBean(Class<T> requiredType) {
        return ApplicationContextHolder.applicationContext.getBean(requiredType);
    }

    public static final <T> T getBean(String name, Class<T> requiredType) {
        return ApplicationContextHolder.applicationContext.getBean(name, requiredType);
    }

    public static final Object getBean(String name) {
        return ApplicationContextHolder.applicationContext.getBean(name);
    }

    public static final <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
        return ApplicationContextHolder.applicationContext.getBeansOfType(requiredType);
    }
}
