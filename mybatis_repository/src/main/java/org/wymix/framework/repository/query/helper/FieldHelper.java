package org.wymix.framework.repository.query.helper;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import org.springframework.util.ReflectionUtils;
import org.wymix.framework.repository.query.annotations.TableAlias;
import org.wymix.framework.utils.DateUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/** Resolves public query properties to trusted, mapped entity fields. */
public final class FieldHelper {
    private FieldHelper() {}

    private static Field field(Class<?> entityClass, String name) {
        if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?")) {
            throw new IllegalArgumentException("Invalid query field: " + name);
        }
        String property = name.substring(name.lastIndexOf('.') + 1);
        Field f = ReflectionUtils.findField(entityClass, property);
        if (f == null
                || Modifier.isStatic(f.getModifiers())
                || Modifier.isTransient(f.getModifiers())
                || f.isSynthetic()
                || (f.isAnnotationPresent(TableField.class)
                        && !f.getAnnotation(TableField.class).exist())) {
            throw new IllegalArgumentException("Unknown or unmapped query field: " + name);
        }
        if (name.contains(".")
                && (!f.isAnnotationPresent(TableAlias.class)
                        || !name.substring(0, name.indexOf('.'))
                                .equals(f.getAnnotation(TableAlias.class).value()))) {
            throw new IllegalArgumentException("Undeclared query alias: " + name);
        }
        return f;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object translate(Class<?> entityClass, String fieldName, String value) {
        Class<?> type = field(entityClass, fieldName).getType();
        if (value == null) return null;
        try {
            if (type.isEnum()) return Enum.valueOf((Class) type, value);
            if (type == Long.class || type == long.class) return Long.valueOf(value);
            if (type == Integer.class || type == int.class) return Integer.valueOf(value);
            if (type == Short.class || type == short.class) return Short.valueOf(value);
            if (type == Double.class || type == double.class) return Double.valueOf(value);
            if (type == Float.class || type == float.class) return Float.valueOf(value);
            if (type == BigDecimal.class) return new BigDecimal(value);
            if (type == Boolean.class || type == boolean.class) {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
                    throw new IllegalArgumentException();
                return Boolean.valueOf(value);
            }
            if (type == LocalDate.class) return LocalDate.parse(value);
            if (type == LocalTime.class) return LocalTime.parse(value);
            if (type == LocalDateTime.class) return LocalDateTime.parse(value.replace(' ', 'T'));
            if (type == Instant.class) return Instant.parse(value);
            if (type == Date.class) {
                Date date = DateUtils.parse(value, DateUtils.YYYYMMDDHHMMSS_LINE);
                if (date == null) throw new IllegalArgumentException();
                return date;
            }
            return value;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Invalid value for query field "
                            + fieldName
                            + " ("
                            + type.getSimpleName()
                            + ")",
                    ex);
        }
    }

    public static String addConditionAlias(
            Class<?> entityClass, String fieldName, String defaultColumn) {
        Field f = field(entityClass, fieldName);
        String column = defaultColumn.substring(defaultColumn.lastIndexOf('.') + 1);
        if (f.isAnnotationPresent(TableId.class)
                && !f.getAnnotation(TableId.class).value().isBlank()) {
            column = f.getAnnotation(TableId.class).value();
        } else if (f.isAnnotationPresent(TableField.class)
                && !f.getAnnotation(TableField.class).value().isBlank()) {
            column = f.getAnnotation(TableField.class).value();
        }
        String alias = "";
        if (f.isAnnotationPresent(TableAlias.class)) {
            TableAlias a = f.getAnnotation(TableAlias.class);
            alias = a.value();
            if (!a.columnAlias().isBlank()) column = a.columnAlias();
        }
        // Annotations are trusted configuration, but may only declare identifiers, not expressions.
        if (!column.matches("[A-Za-z_][A-Za-z0-9_]*")
                || (!alias.isBlank() && !alias.matches("[A-Za-z_][A-Za-z0-9_]*"))) {
            throw new IllegalArgumentException(
                    "Query mapping must contain SQL identifiers only: " + fieldName);
        }
        return alias.isBlank() ? column : alias + "." + column;
    }
}
