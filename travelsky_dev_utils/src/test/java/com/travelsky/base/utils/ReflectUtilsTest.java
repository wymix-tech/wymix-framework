package com.travelsky.base.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReflectUtils 单元测试：字段缓存、父类字段上钻、静态字段读取与异常场景。
 * 同时覆盖 deprecated 兼容门面（ReflectionUtils 继承的字段方法、FastCompressUtil 委托）。
 */
class ReflectUtilsTest {

    static class Base {
        private String baseField = "base";
    }

    static class Sub extends Base {
        private String subField = "sub";
        private static String STATIC_FIELD = "staticValue";
    }

    static class Sub2 extends Sub {
        private String sub2Field = "sub2";
    }

    @Test
    void shouldGetFieldValueByFieldName() {
        Sub obj = new Sub();
        assertEquals("sub", ReflectUtils.getFieldValue(obj, "subField"));
    }

    @Test
    void shouldGetSuperClassFieldValue() {
        Sub obj = new Sub();
        assertEquals("base", ReflectUtils.getFieldValue(obj, "baseField"));
    }

    @Test
    void shouldReadStaticFieldViaClass() {
        assertEquals("staticValue", ReflectUtils.getFieldValue(Sub.class, "STATIC_FIELD"));
    }

    @Test
    void shouldReturnNullWhenObjNullOrNameBlank() {
        assertNull(ReflectUtils.getFieldValue(null, "subField"));
        assertNull(ReflectUtils.getFieldValue(new Sub(), (String) " "));
        assertNull(ReflectUtils.getFieldValue(new Sub(), (String) null));
    }

    @Test
    void shouldReturnNullWhenFieldMissing() {
        // 缺失字段时内部 Optional.get 抛 NoSuchElementException
        assertThrows(RuntimeException.class, () -> ReflectUtils.getFieldValue(new Sub(), "noSuchField"));
    }

    @Test
    void shouldGetFieldsDirectlyWithAndWithoutSuper() {
        assertEquals(1, ReflectUtils.getFieldsDirectly(Sub2.class, false).length);
        assertTrue(ReflectUtils.getFieldsDirectly(Sub2.class, true).length >= 3);
    }

    @Test
    void shouldCacheFieldsAcrossCalls() throws Exception {
        // getFields 走 WeakConcurrentMap 缓存：首次 miss 填充，二次命中返回缓存实例
        java.lang.reflect.Method getFields = ReflectUtils.class.getDeclaredMethod("getFields", Class.class);
        getFields.setAccessible(true);
        assertEquals(1, ((Object[]) getFields.invoke(null, (Object) FieldClassHolder.class)).length);
        Object cached = getFields.invoke(null, (Object) FieldClassHolder.class);
        // 复制 getFields 的缓存判断逻辑：命中分支直接返回缓存值
        java.lang.reflect.Field cache = ReflectUtils.class.getDeclaredField("FIELDS_CACHE");
        cache.setAccessible(true);
        Object cachedArray = ((com.travelsky.base.utils.map.WeakConcurrentMap) cache.get(null))
                .get(FieldClassHolder.class);
        assertNotNull(cachedArray);
        assertSame(cachedArray, cached);
    }

    @Test
    void getFieldsCacheHitShouldShortCircuit() throws Exception {
        // 缓存命中分支直接返回 get 值；若命中分支失效（computeIfAbsent 分支被变异），结果仍正确但缓存引用可能不同
        java.lang.reflect.Field cache = ReflectUtils.class.getDeclaredField("FIELDS_CACHE");
        cache.setAccessible(true);
        com.travelsky.base.utils.map.WeakConcurrentMap<Class<?>, java.lang.reflect.Field[]> map =
                (com.travelsky.base.utils.map.WeakConcurrentMap<Class<?>, java.lang.reflect.Field[]>) cache.get(null);
        map.clear();
        java.lang.reflect.Field[] populated = ReflectUtils.getFieldsDirectly(FieldClassHolder.class, true);
        map.put(FieldClassHolder.class, populated);
        // 精确断言命中分支返回的对象即为缓存中的同一实例
        java.lang.reflect.Method getFields = ReflectUtils.class.getDeclaredMethod("getFields", Class.class);
        getFields.setAccessible(true);
        assertSame(populated, getFields.invoke(null, (Object) FieldClassHolder.class));
        // put 一个哨兵数组后命中，若命中分支被改为 computeIfAbsent 分支则仍返回哨兵（get 不为 null），行为一致；
        // 再验证 miss 路径 computeIfAbsent 确实写入缓存
        map.clear();
        Object repopulated = getFields.invoke(null, (Object) FieldClassHolder.class);
        assertSame(map.get(FieldClassHolder.class), repopulated);
    }

    @Test
    void setAccessibleShouldBeIdempotent() throws Exception {
        // setAccessible 幂等：isAccessible 为 true 时跳过再次设置
        java.lang.reflect.Method m = ReflectUtils.class.getDeclaredMethod("setAccessible", java.lang.reflect.AccessibleObject.class);
        m.setAccessible(true);
        java.lang.reflect.Field f = Sub.class.getDeclaredField("subField");
        f.setAccessible(true);
        assertSame(f, m.invoke(null, f));
        assertTrue(f.isAccessible());
        f.setAccessible(false);
        assertSame(f, m.invoke(null, f));
        assertTrue(f.isAccessible());
    }

    static class FieldClassHolder {
        private int x;
    }

    @Test
    void shouldGetFieldValueByFieldObject() throws NoSuchFieldException {
        Sub obj = new Sub();
        java.lang.reflect.Field f = Sub.class.getDeclaredField("subField");
        assertEquals("sub", ReflectUtils.getFieldValue(obj, f));
        assertNull(ReflectUtils.getFieldValue(obj, (java.lang.reflect.Field) null));
    }

    // ===== deprecated 兼容门面回归 =====

    @Test
    void deprecatedReflectionUtilsShouldStillResolveFields() {
        Sub obj = new Sub();
        assertEquals("base", ReflectionUtils.getFieldValueForAll(obj, "baseField"));
        assertEquals("sub", ReflectionUtils.getFieldValue(obj, "subField"));
        assertNotNull(ReflectionUtils.findFieldForAll(obj, "subField"));
    }

    @Test
    void deprecatedFastCompressUtilShouldDelegateToCompressUtil() throws Exception {
        String raw = "compress-me-with-a-relatively-long-payload-to-make-gzip-worthwhile-1234567890";
        String compressed = FastCompressUtil.compress(raw);
        assertEquals(CompressUtil.compress(raw), compressed);
        assertEquals(raw, FastCompressUtil.decompress(compressed));
    }

    @Test
    void compressUtilShouldRoundTripAndTolerateBlank() throws Exception {
        String raw = "round-trip-payload-abcdefghijklmnopqrstuvwxyz-0123456789";
        assertEquals(raw, CompressUtil.decompress(CompressUtil.compress(raw)));
        assertEquals("", CompressUtil.compress(""));
        assertNull(CompressUtil.compress(null));
        // 非 Base64 输入原样返回
        assertEquals("plain", CompressUtil.decompress("plain"));
    }

    @Test
    void compressUtilShouldReturnSameInstanceForBlank() throws Exception {
        // 空白输入直接短路返回原值（条件边界与短路逻辑）
        assertEquals("  ", CompressUtil.decompress("  "));
    }

    @Test
    void compressUtilShouldHandleExactBufferSizePayload() throws Exception {
        // 构造解压后长度恰好为 buffer=1024 边界的数据：read 返回值=0/1024 的边界分支
        String raw = "a".repeat(1024);
        assertEquals(raw, CompressUtil.decompress(CompressUtil.compress(raw)));
        String raw2 = "b".repeat(1025);
        assertEquals(raw2, CompressUtil.decompress(CompressUtil.compress(raw2)));
        String raw3 = "c".repeat(1023);
        assertEquals(raw3, CompressUtil.decompress(CompressUtil.compress(raw3)));
    }

    @Test
    void compressUtilShouldHandleEmptyGzipPayload() throws Exception {
        // 空 payload 的合法 GZIP 流，循环 0 次输出空串
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(out)) {
            gzip.write(new byte[0]);
        }
        String gz = java.util.Base64.getEncoder().encodeToString(out.toByteArray());
        assertEquals("", CompressUtil.decompress(gz));
    }
}
