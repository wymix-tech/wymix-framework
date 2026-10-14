package org.wymix.framework.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.junit.jupiter.api.Test;
import org.wymix.framework.utils.json.NumberSerializer;
import org.wymix.framework.utils.map.SafeConcurrentHashMap;

class FrameworkContractsTest {
    static class Base<E, V> {}

    static class Middle<S> extends Base<String, Long> {}

    static class Concrete extends Middle<Integer> {}

    @Test
    void resolvesSpecifiedBaseThroughIntermediateGenericClass() {
        assertEquals(
                String.class, ReflectUtils.getSuperClassGenericType(Concrete.class, Base.class, 0));
        assertEquals(
                Long.class, ReflectUtils.getSuperClassGenericType(Concrete.class, Base.class, 1));
    }

    @Test
    void mapSupportsNullComputationsAndCachesOnce() {
        var map = new SafeConcurrentHashMap<String, String>();
        assertNull(map.computeIfAbsent("missing", k -> null));
        assertFalse(map.containsKey("missing"));
        assertEquals("one", map.computeIfAbsent("key", k -> "one"));
        assertEquals("one", map.computeIfAbsent("key", k -> "two"));
    }

    @Test
    void protectsBothSignsAndHandlesNonFiniteFloats() throws Exception {
        var mapper = new ObjectMapper();
        var module = new SimpleModule();
        module.addSerializer(Number.class, new NumberSerializer(Number.class));
        mapper.registerModule(module);
        assertEquals("\"-9007199254740993\"", mapper.writeValueAsString(-9007199254740993L));
        assertEquals("\"9007199254740993\"", mapper.writeValueAsString(9007199254740993L));
        assertEquals("42", mapper.writeValueAsString(42L));
        assertDoesNotThrow(() -> mapper.writeValueAsString(Double.NaN));
    }
}
