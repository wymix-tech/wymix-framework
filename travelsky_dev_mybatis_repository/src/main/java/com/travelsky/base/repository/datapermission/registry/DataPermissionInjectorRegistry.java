package com.travelsky.base.repository.datapermission.registry;

import com.travelsky.base.repository.datapermission.annotations.EnableDataPermission;
import com.travelsky.base.repository.datapermission.aop.DataPermissionAspect;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;
import com.travelsky.base.repository.datapermission.plugin.DataPermissionPlugin;
import com.travelsky.base.utils.ReflectionUtils;
import com.travelsky.base.utils.StringUtils;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.Field;
import java.util.Set;

public class DataPermissionInjectorRegistry implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, ApplicationContextAware {

    private static final String SETTER = "public void set%s(%s %s) { this.%s = %s; }";

    private ResourceLoader resourceLoader;

    private ApplicationContext applicationContext;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(EnableDataPermission.class.getName()));
        String[] basePackages = annoAttrs.getStringArray("basePackages");

        Reflections reflections;
        if (basePackages == null || basePackages.length == 0) {
            reflections = new Reflections();
        } else {
            reflections = new Reflections(basePackages);
        }
        Set<Class<? extends PermissionSqlInjector>> subTypesOf = reflections.getSubTypesOf(PermissionSqlInjector.class);
        BeanDefinitionBuilder builder;
        for (Class<? extends PermissionSqlInjector> clz : subTypesOf) {
            try {
                Field[] allFieldByClazz = ReflectionUtils.findFieldWithAnnotation(clz, Autowired.class);
                builder = BeanDefinitionBuilder.genericBeanDefinition(clz);
                builder.setScope(BeanDefinition.SCOPE_SINGLETON);
                for (Field fieldByClazz : allFieldByClazz) {
                    if (fieldByClazz.isAnnotationPresent(Autowired.class))
                        builder.addAutowiredProperty(fieldByClazz.getName());
                }
                registry.registerBeanDefinition(StringUtils.firstToLowerCase(clz.getSimpleName()), builder.getBeanDefinition());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        builder = BeanDefinitionBuilder
                .genericBeanDefinition(DataPermissionPlugin.class);
        registry.registerBeanDefinition(StringUtils.firstToLowerCase(DataPermissionPlugin.class.getSimpleName()), builder.getBeanDefinition());

        builder = BeanDefinitionBuilder
                .genericBeanDefinition(DataPermissionAspect.class);
        registry.registerBeanDefinition(StringUtils.firstToLowerCase(DataPermissionAspect.class.getSimpleName()), builder.getBeanDefinition());


    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
