package org.wymix.framework.cache.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缓存名归一化规则单测：分隔符与大小写不敏感。
 */
class CacheNameUtilsTest {

    @Test
    void shouldIgnoreCaseAndSeparators() {
        assertThat(CacheNameUtils.normalize("SampleCache")).isEqualTo("samplecache");
        assertThat(CacheNameUtils.normalize("sample-cache")).isEqualTo("samplecache");
        assertThat(CacheNameUtils.normalize("sample_cache")).isEqualTo("samplecache");
        assertThat(CacheNameUtils.normalize("sample.cache")).isEqualTo("samplecache");
    }

    @Test
    void shouldKeepOtherCharacters() {
        assertThat(CacheNameUtils.normalize("user:1")).isEqualTo("user:1");
        assertThat(CacheNameUtils.normalize("缓存A")).isEqualTo("缓存a");
    }

    @Test
    void nullShouldStayNull() {
        assertThat(CacheNameUtils.normalize(null)).isNull();
    }
}
