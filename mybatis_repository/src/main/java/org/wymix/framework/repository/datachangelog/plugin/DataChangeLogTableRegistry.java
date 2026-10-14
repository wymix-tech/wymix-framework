package org.wymix.framework.repository.datachangelog.plugin;


import com.baomidou.mybatisplus.annotation.TableName;
import org.wymix.framework.repository.datachangelog.annotation.DataChangeLog;
import org.wymix.framework.repository.datamigration.annotations.DataMigration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DataChangeLogTableRegistry {

    private static final ConcurrentHashMap<String, Class> TABLES = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Class, String> CLASSES = new ConcurrentHashMap<>();
    private String[] basePackages;

    public static boolean exists(String tableName) {
        return TABLES.containsKey(tableName.toLowerCase(java.util.Locale.ROOT));
    }

    public static boolean exists(Class tableClass) {
        return CLASSES.containsKey(tableClass);
    }

    public static Class getClass(String tableName) {
        return TABLES.get(tableName.toLowerCase(java.util.Locale.ROOT));
    }

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }

    @PostConstruct
    private void init() {
        Reflections reflections;
        if (basePackages == null || basePackages.length == 0) {
            reflections = new Reflections();
        } else {
            reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages(basePackages) // 替换为您的包名
                    .addScanners(Scanners.TypesAnnotated));
        }
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(DataChangeLog.class);
        classes.addAll(reflections.getTypesAnnotatedWith(DataMigration.class));
        classes.stream().filter(clazz -> clazz.isAnnotationPresent(TableName.class))
                .filter(clz -> !Modifier.isAbstract(clz.getModifiers()))
                .forEach(clz -> {
                    TableName tableName = clz.getAnnotation(TableName.class);
                    TABLES.put(tableName.value().toLowerCase(java.util.Locale.ROOT), clz);
                    CLASSES.put(clz, tableName.value());
                });
    }

}
