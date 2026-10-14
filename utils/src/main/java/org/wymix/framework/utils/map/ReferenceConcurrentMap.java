package org.wymix.framework.utils.map;

import org.springframework.util.ConcurrentReferenceHashMap;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReferenceConcurrentMap<K, V> implements ConcurrentMap<K, V>, Iterable<Map.Entry<K, V>>, Serializable {

    final ConcurrentMap<Reference<K>, V> raw;
    private final ReferenceQueue<K> lastQueue;
    private final ConcurrentReferenceHashMap.ReferenceType keyType;

    private BiConsumer<Reference<? extends K>, V> purgeListener;

    public ReferenceConcurrentMap(ConcurrentMap<Reference<K>, V> raw, ConcurrentReferenceHashMap.ReferenceType keyType) {
        this.raw = raw;
        this.keyType = keyType;
        lastQueue = new ReferenceQueue<>();
    }

    public void setPurgeListener(BiConsumer<Reference<? extends K>, V> purgeListener) {
        this.purgeListener = purgeListener;
    }

    @Override
    public int size() {
        this.purgeStaleKeys();
        return this.raw.size();
    }

    @Override
    public boolean isEmpty() {
        return 0 == this.size();
    }

    @Override
    public V get(Object key) {
        this.purgeStaleKeys();
        return this.raw.get(ofKey((K) key, null));
    }

    @Override
    public V putIfAbsent(K key, V value) {
        this.purgeStaleKeys();
        return this.raw.putIfAbsent(ofKey(key, this.lastQueue), value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        this.purgeStaleKeys();
        return this.raw.remove(ofKey((K) key, null), value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        this.purgeStaleKeys();
        return this.raw.replace(ofKey(key, lastQueue), oldValue, newValue);
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        this.purgeStaleKeys();
        this.raw.replaceAll((kWeakKey, value) -> function.apply(kWeakKey.get(), value));
    }

    @Override
    public V replace(K key, V value) {
        this.purgeStaleKeys();
        return this.raw.replace(ofKey(key, lastQueue), value);
    }

    @Override
    public boolean containsKey(Object key) {
        this.purgeStaleKeys();
        return this.raw.containsKey(ofKey((K) key, null));
    }

    @Override
    public boolean containsValue(Object value) {
        this.purgeStaleKeys();
        return this.raw.containsValue(value);
    }

    @Override
    public V put(K key, V value) {
        this.purgeStaleKeys();
        return this.raw.put(ofKey(key, this.lastQueue), value);
    }

    @Override
    public V remove(Object key) {
        this.purgeStaleKeys();
        return this.raw.remove(ofKey((K) key, null));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        this.raw.clear();
        while (lastQueue.poll() != null) ;
    }

    @Override
    public Set<K> keySet() {
        this.purgeStaleKeys();
        return this.raw.keySet().stream().map(Reference::get).collect(Collectors.toSet());
    }

    @Override
    public Collection<V> values() {
        this.purgeStaleKeys();
        return this.raw.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        this.purgeStaleKeys();
        return this.raw.entrySet().stream().map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey().get(), entry.getValue())).collect(Collectors.toSet());
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        this.purgeStaleKeys();
        return this.raw.computeIfAbsent(ofKey(key, this.lastQueue), kWeakKey -> mappingFunction.apply(key));
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        this.purgeStaleKeys();
        return this.raw.computeIfPresent(ofKey(key, this.lastQueue), (kWeakKey, value) -> remappingFunction.apply(key, value));
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        this.purgeStaleKeys();
        this.raw.forEach((key, value) -> action.accept(key.get(), value));
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return entrySet().iterator();
    }

    @Override
    public void forEach(Consumer<? super Entry<K, V>> action) {
        this.purgeStaleKeys();
        this.raw.entrySet().forEach(e -> action.accept(new AbstractMap.SimpleImmutableEntry<>(e.getKey().get(), e.getValue())));
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        this.purgeStaleKeys();
        return this.raw.compute(ofKey(key, this.lastQueue), (kWeakKey, value) -> remappingFunction.apply(key, value));
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        this.purgeStaleKeys();
        return this.raw.merge(ofKey(key, this.lastQueue), value, remappingFunction);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        this.purgeStaleKeys();
        return this.raw.getOrDefault(ofKey((K) key, null), defaultValue);
    }

    private void purgeStaleKeys() {
        Reference<? extends K> reference;
        V value;
        while ((reference = this.lastQueue.poll()) != null) {
            value = this.raw.remove(reference);
            if (null != purgeListener) {
                purgeListener.accept(reference, value);
            }
        }
    }

    private Reference<K> ofKey(K key, ReferenceQueue<? super K> queue) {
        switch (this.keyType) {
            case SOFT:
                return new SoftKey<>(key, queue);
            case WEAK:
                return new WeakKey<>(key, queue);
        }
        throw new IllegalArgumentException("Unsupported key type: " + keyType);
    }


    private static class WeakKey<K> extends WeakReference<K> {
        private final int hashcode;

        public WeakKey(K referent, ReferenceQueue<? super K> q) {
            super(referent, q);
            this.hashcode = referent.hashCode();
        }

        @Override
        public int hashCode() {
            return this.hashcode;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other instanceof WeakKey) {
                return Objects.equals(((WeakKey<?>) other).get(), this.get());
            }
            return false;
        }
    }

    private static class SoftKey<K> extends SoftReference<K> {
        private final int hashcode;

        public SoftKey(K referent, ReferenceQueue<? super K> q) {
            super(referent, q);
            this.hashcode = referent.hashCode();
        }

        @Override
        public int hashCode() {
            return this.hashcode;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other instanceof SoftKey) {
                return Objects.equals(((SoftKey<?>) other).get(), this.get());
            }
            return false;
        }
    }
}
