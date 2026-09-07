package com.travelsky.base.utils;

import com.travelsky.base.utils.map.WeakConcurrentMap;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.Arrays;


/**
 * 反射字段工具。
 *
 * <p>历史背景：本类与 {@link ReflectionUtils} 功能重叠且互相引用，已收敛为唯一实现，
 * {@link ReflectionUtils} 保留为 deprecated 兼容门面，新代码请直接使用本类。</p>
 */
public class ReflectUtils {

    private static final WeakConcurrentMap<Class<?>, Field[]> FIELDS_CACHE = new WeakConcurrentMap<>();

    protected ReflectUtils() {
    }

    public static Field[] getFieldsDirectly(Class<?> clz, boolean withSuperClassFields) {
        Field[] allFields = null;
        Class<?> searchType = clz;
        Field[] declaredFields;
        while (searchType != null) {
            declaredFields = searchType.getDeclaredFields();
            if (null == allFields) {
                allFields = declaredFields;
            } else {
                allFields = ArrayUtils.addAll(allFields, declaredFields);
            }
            searchType = withSuperClassFields ? searchType.getSuperclass() : null;
        }
        return allFields;
    }

    public static Object getFieldValue(Object obj, Field field) {
        if (field == null) {
            return null;
        }
        if (obj instanceof Class) {
            obj = null;
        }

        setAccessible(field);
        Object result;
        try {
            result = field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static Object getFieldValue(Object obj, String fieldName) {
        if (StringUtils.isBlank(fieldName) || obj == null) {
            return null;
        }
        return getFieldValue(obj, getField(obj instanceof Class ? (Class<?>) obj : obj.getClass(), fieldName));
    }

    private static Field getField(Class<?> aClass, String fieldName) {
        final Field[] fields = getFields(aClass);
        return Arrays.stream(fields).filter(field -> field.getName().equals(fieldName)).findFirst().get();
    }

    private static Field[] getFields(Class<?> aClass) {
        if (FIELDS_CACHE.get(aClass) == null) {
            return FIELDS_CACHE.computeIfAbsent(aClass, (a) -> getFieldsDirectly(a, true));
        }
        return FIELDS_CACHE.get(aClass);
    }

    private static <T extends AccessibleObject> T setAccessible(T accessibleObject) {
        if (null != accessibleObject && !accessibleObject.isAccessible()) {
            accessibleObject.setAccessible(true);
        }
        return accessibleObject;
    }
}
