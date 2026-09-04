package com.travelsky.base.repository.datachangelog.supports;

import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManualDmlContext {

    private static final ThreadLocal<Pair<Map<String, Object>, Map<String, Object>>> MANUAL_DML = ThreadLocal.withInitial(() -> null);

    public static boolean has() {
        return MANUAL_DML.get() != null;
    }

    public static Map<String, Object> getParams() {
        return MANUAL_DML.get().getKey();
    }

    public static Map<String, Object> createParams() {
        HashMap<String, Object> params = new HashMap<>();
        MANUAL_DML.set(Pair.of(params, new HashMap<>()));
        return params;
    }

    public static void returnResults(PlainSelect sql, Class entityClass, List<Object> results) {
        Map<String, Object> map = MANUAL_DML.get().getValue();
        map.put("sql", sql);
        map.put("entityClass", entityClass);
        map.put("results", results);
    }

    public static Map<String, Object> getResults() {
        return MANUAL_DML.get().getValue();
    }

    public static void clear() {
        MANUAL_DML.remove();
    }

}
