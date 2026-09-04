package com.travelsky.base.utils.map;

import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.ref.Reference;
import java.util.concurrent.ConcurrentMap;

public class WeakConcurrentMap<K, V> extends ReferenceConcurrentMap<K, V> {

    public WeakConcurrentMap() {
        this(new SafeConcurrentHashMap<>());
    }

    public WeakConcurrentMap(ConcurrentMap<Reference<K>, V> raw) {
        super(raw, ConcurrentReferenceHashMap.ReferenceType.WEAK);

    }
}
