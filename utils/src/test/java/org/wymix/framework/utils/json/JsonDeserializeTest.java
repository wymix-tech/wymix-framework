package org.wymix.framework.utils.json;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonDeserialize 单元测试：覆盖正常反序列化与非法输入容错（printStackTrace -> slf4j 改造后行为不变）。
 */
class JsonDeserializeTest {

    static class SampleList extends JsonDeserialize<List<String>> {
    }

    @Test
    void shouldDeserializeJsonToList() {
        List<String> result = new SampleList().jsonToEntity("[\"a\",\"b\"]");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        assertNull(new SampleList().jsonToEntity(null));
    }

    @Test
    void shouldReturnNullWhenInputIsInvalidJson() {
        // 非法 JSON 不抛异常，返回 null（内部 warn 日志）
        assertNull(new SampleList().jsonToEntity("not-a-json"));
    }

    @Test
    void shouldDeserializeNestedType() {
        class Nested extends JsonDeserialize<List<Integer>> {
        }
        List<Integer> result = new Nested().jsonToEntity("[1,2,3]");
        assertEquals(3, result.size());
        assertEquals(2, result.get(1));
    }
}
