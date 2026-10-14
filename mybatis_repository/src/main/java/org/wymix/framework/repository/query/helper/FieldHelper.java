package org.wymix.framework.repository.query.helper;

import org.wymix.framework.repository.query.annotations.TableAlias;
import org.wymix.framework.utils.DateUtils;
import org.wymix.framework.utils.StringUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;

public class FieldHelper {

    public static Object translate(@SuppressWarnings("rawtypes") Class entityClass, String fieldName, String value) {
        Field field = ReflectionUtils.findField(entityClass, fieldName);
        if (field == null) {
            return value;
        }
        if (value == null) {
            return null;
        }
        // 字段属性
        @SuppressWarnings("rawtypes")
        Class clazz = field.getType();
        if (field.getType().isEnum()) {
            try {
                return Enum.valueOf(clazz, value);
            } catch (Exception ex) {
            }
        }
        if (clazz.equals(Date.class)) {
            return DateUtils.parse(value, DateUtils.YYYYMMDDHHMMSS_LINE);
        }
        if (clazz.equals(Long.class)) {
            try {
                return Long.valueOf(value);
            } catch (Exception ex) {
                return null;
            }
        }
        if (clazz.equals(Integer.class)) {
            try {
                return Integer.valueOf(value);
            } catch (Exception ex) {
                return null;
            }
        }
        if (clazz.equals(BigDecimal.class)) {
            try {
                return new BigDecimal(value);
            } catch (Exception ex) {
                return null;
            }
        }
        return value;
    }

    public static String addConditionAlias(@SuppressWarnings("rawtypes") Class entityClass, String fieldName, String columnName) {
        String fieldNameAlias = columnName;
        Field f = ReflectionUtils.findField(entityClass, fieldName);
        if (f != null && f.isAnnotationPresent(TableAlias.class)) {
            TableAlias a = f.getAnnotation(TableAlias.class);
            String tableAlias = a.value();
            String columnAlias = StringUtils.isEmpty(a.columnAlias()) ? columnName : a.columnAlias();
            if (StringUtils.isNotEmpty(tableAlias)) {
                fieldNameAlias = tableAlias + "." + columnAlias;
            } else {
                fieldNameAlias = columnAlias;
            }
        }
        return fieldNameAlias;
    }
}
