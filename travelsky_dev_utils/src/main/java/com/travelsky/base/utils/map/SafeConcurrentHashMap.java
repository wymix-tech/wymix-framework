package com.travelsky.base.utils.map;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class SafeConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {

    public SafeConcurrentHashMap() {
        super();
    }

    public SafeConcurrentHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public SafeConcurrentHashMap(Map<? extends K, ? extends V> map) {
        super(map);
    }

    public SafeConcurrentHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public SafeConcurrentHashMap(int initialCapacity, float loadFactor, int concurrentLevel) {
        super(initialCapacity, loadFactor, concurrentLevel);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V value = this.get(key);
        if (null == value) {
            value = mappingFunction.apply(key);
            final V res = this.putIfAbsent(key, value);
            if (null != res) {
                return res;
            }
        }
        return value;
    }

}
