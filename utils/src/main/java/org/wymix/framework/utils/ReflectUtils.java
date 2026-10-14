package org.wymix.framework.utils;

import org.wymix.framework.utils.map.WeakConcurrentMap;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * 反射工具（框架内唯一实现）：字段读取与缓存、泛型实参解析、按注解查找字段。
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

    /**
     * 反射对象获取泛型实参类型；若泛型为 {@code List<X>} 则返回 {@code List} 原始类型。
     *
     * @param clazz 对象类型
     * @param index 泛型所在位置
     * @return 泛型实参对应的 Class，无法解析时返回 {@code Object.class}
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getSuperClassGenericType(final Class<?> clazz, final int index) {
        Type genType = clazz.getGenericSuperclass();
        Class<?> superClass = clazz;
        while (!(genType instanceof ParameterizedType)) {
            if (superClass == Object.class) {
                return (Class<T>) Object.class;
            }
            superClass = superClass.getSuperclass();
            genType = superClass.getGenericSuperclass();
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        if (index >= params.length || index < 0) {
            return (Class<T>) Object.class;
        }
        if (!(params[index] instanceof Class) && params[index] instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) params[index];
            return (Class<T>) pType.getRawType();
        }
        return (Class<T>) params[index];
    }

    /**
     * 按泛型所在位置实例化对应的类。
     *
     * @param clazz 对象类型
     * @param index 泛型所在位置
     * @return 泛型实参类的新实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(final Class<?> clazz, final int index) {
        try {
            return (T) getSuperClassGenericType(clazz, index).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("newInstance error", e);
        }
    }

    /**
     * 获取类及其父类的全部字段。
     *
     * @param clazz 类型
     * @return 字段列表
     */
    public static List<Field> getAllFieldByClazz(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        Class<?> tempClass = clazz;
        while (tempClass != null) {
            fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
            tempClass = tempClass.getSuperclass();
        }
        return fieldList;
    }

    /**
     * 查找标注了指定注解的全部字段（含父类字段）。
     *
     * @param clazz          类型
     * @param annotationClass 注解类型
     * @return 命中的字段数组
     */
    public static Field[] findFieldWithAnnotation(Class<?> clazz, Class<? extends java.lang.annotation.Annotation> annotationClass) {
        return getAllFieldByClazz(clazz).stream()
                .filter(field -> field.isAnnotationPresent(annotationClass))
                .toArray(Field[]::new);
    }
}
