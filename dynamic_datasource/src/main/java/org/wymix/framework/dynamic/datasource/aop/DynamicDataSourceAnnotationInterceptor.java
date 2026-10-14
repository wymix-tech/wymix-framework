/*
 * Copyright © 2018 organization baomidou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wymix.framework.dynamic.datasource.aop;

import org.wymix.framework.common.exception.BusinessException;
import org.wymix.framework.dynamic.datasource.annotations.DynamicDatasource;
import org.wymix.framework.dynamic.datasource.support.DynamicDataSourceContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodClassKey;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicDataSourceAnnotationInterceptor implements MethodInterceptor {

    private static boolean ENABLE_MYBATIS = false;
    private static Field mapperInterfaceField;

    static {
        Class<?> proxyClass = null;
        try {
            proxyClass = Class.forName("com.baomidou.mybatisplus.core.override.MybatisMapperProxy");
        } catch (ClassNotFoundException e1) {
            try {
                proxyClass = Class.forName("com.baomidou.mybatisplus.core.override.PageMapperProxy");
            } catch (ClassNotFoundException e2) {
                try {
                    proxyClass = Class.forName("org.apache.ibatis.binding.MapperProxy");
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        if (proxyClass != null) {
            try {
                mapperInterfaceField = proxyClass.getDeclaredField("mapperInterface");
                mapperInterfaceField.setAccessible(true);
                ENABLE_MYBATIS = true;
            } catch (NoSuchFieldException e) {
                
            }
        }
    }

    private final Map<Object, String> dsCache = new ConcurrentHashMap<>();

    public DynamicDataSourceAnnotationInterceptor() {

    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String datasource = determineDatasourceKey(invocation);
        if (!DynamicDataSourceContextHolder.containsDataSource(datasource)) {
            throw new BusinessException("datasource.not.exist");
        }
        String previous = DynamicDataSourceContextHolder.getDataSource();
        DynamicDataSourceContextHolder.setDataSource(datasource);
        try {
            return invocation.proceed();
        } finally {
            if (previous == null) {
                DynamicDataSourceContextHolder.clear();
            } else {
                DynamicDataSourceContextHolder.setDataSource(previous);
            }
        }
    }

    private String determineDatasourceKey(MethodInvocation invocation) {
        return findKey(invocation.getMethod(),invocation.getThis(), DynamicDatasource.class);
    }

    public String findKey(Method method, Object targetObject, Class<? extends Annotation> annotation) {
        if (method.getDeclaringClass() == Object.class) {
            return "";
        }
        Object cacheKey = new MethodClassKey(method, targetObject.getClass());
        String ds = this.dsCache.get(cacheKey);
        if (ds == null) {
            Optional<String> dsOperation = computeDatasource(method, targetObject, annotation);
            ds = dsOperation.orElse("");
            this.dsCache.put(cacheKey, ds);
        }
        return ds;
    }

    private <T> Optional<T> computeDatasource(Method method, Object targetObject, Class<? extends Annotation> annotation) {
        //1. 从当前方法接口中获取
        Optional<T> dsAttr = findDataSourceAttribute(method, annotation);
        if (dsAttr.isPresent()) {
            return dsAttr;
        }
        Class<?> targetClass = targetObject.getClass();
        Class<?> userClass = ClassUtils.getUserClass(targetClass);
        // JDK代理时,  获取实现类的方法声明.  method: 接口的方法, specificMethod: 实现类方法
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, userClass);

        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        //2. 从实现类的方法找
        dsAttr = findDataSourceAttribute(specificMethod, annotation);
        if (dsAttr.isPresent()) {
            return dsAttr;
        }
        // 从当前方法声明的类查找
        dsAttr = findDataSourceAttribute(userClass, annotation);
        if (dsAttr.isPresent() && ClassUtils.isUserLevelMethod(method)) {
            return dsAttr;
        }
        //since 3.4.1 从接口查找，只取第一个找到的
        for (Class<?> interfaceClazz : ClassUtils.getAllInterfacesForClassAsSet(userClass)) {
            dsAttr = findDataSourceAttribute(interfaceClazz, annotation);
            if (dsAttr.isPresent()) {
                return dsAttr;
            }
        }
        // 如果存在桥接方法
        if (specificMethod != method) {
            // 从桥接方法查找
            dsAttr = findDataSourceAttribute(method, annotation);
            if (dsAttr.isPresent()) {
                return dsAttr;
            }
            // 从桥接方法声明的类查找
            dsAttr = findDataSourceAttribute(method.getDeclaringClass(), annotation);
            if (dsAttr.isPresent() && ClassUtils.isUserLevelMethod(method)) {
                return dsAttr;
            }
        }
        return getDefaultDataSourceAttr(targetObject, annotation);
    }

    private <T> Optional<T> getDefaultDataSourceAttr(Object targetObject, Class<? extends Annotation> annotation) {
        Class<?> targetClass = targetObject.getClass();
        // 如果不是代理类, 从当前类开始, 不断的找父类的声明
        if (!Proxy.isProxyClass(targetClass)) {
            Class<?> currentClass = targetClass;
            // 接口（如 MyBatis Mapper）的 getSuperclass() 为 null，须判空避免 NPE
            while (currentClass != null && currentClass != Object.class) {
                Optional<T> datasourceAttr = findDataSourceAttribute(currentClass, annotation);
                if (datasourceAttr.isPresent()) {
                    return datasourceAttr;
                }
                currentClass = currentClass.getSuperclass();
            }
        }
        // mybatis-plus, mybatis-spring 的获取方式
        if (ENABLE_MYBATIS) {
            final Class<?> clazz = getMapperInterfaceClass(targetObject);
            if (clazz != null) {
                Optional<T> datasourceAttr = findDataSourceAttribute(clazz, annotation);
                if (datasourceAttr.isPresent()) {
                    return datasourceAttr;
                }
                // 尝试从其父接口获取（Mapper 接口为 interface，getSuperclass() 恒为 null，须遍历父接口）
                for (Class<?> superInterface : clazz.getInterfaces()) {
                    Optional<T> superAttr = findDataSourceAttribute(superInterface, annotation);
                    if (superAttr.isPresent()) {
                        return superAttr;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Class<?> getMapperInterfaceClass(Object target) {
        Object current = target;
        while (Proxy.isProxyClass(current.getClass())) {
            Object currentRefObject = AopProxyUtils.getSingletonTarget(current);
            if (currentRefObject == null) {
                break;
            }
            current = currentRefObject;
        }
        try {
            if (Proxy.isProxyClass(current.getClass())) {
                return (Class<?>) mapperInterfaceField.get(Proxy.getInvocationHandler(current));
            }
        } catch (IllegalAccessException ignore) {
        }
        return null;
    }

    private <T> Optional<T> findDataSourceAttribute(AnnotatedElement ae, Class<? extends Annotation> annotation) {
        if (annotation.isAssignableFrom(DynamicDatasource.class)) {
            DynamicDatasource ds = AnnotatedElementUtils.findMergedAnnotation(ae, DynamicDatasource.class);
            if (ds != null) {
                return (Optional<T>) Optional.of(ds.value());
            }
        }
        return Optional.empty();
    }
}