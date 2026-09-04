package com.travelsky.base.dynamic.datasource.support;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class DynamicDataSourceContextHolder {
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();
    private static final Set<String> DATASOURCE_NAMES = new CopyOnWriteArraySet<>();

    public static void setDataSource(String name) {
        CONTEXT_HOLDER.set(name);
    }

    public static String getDataSource() {
        return CONTEXT_HOLDER.get();
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    public static void addDataSource(String name) {
        DATASOURCE_NAMES.add(name);
    }

    public static boolean containsDataSource(String name) {
        return DATASOURCE_NAMES.contains(name);
    }
}
