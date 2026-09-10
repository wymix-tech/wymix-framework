package com.travelsky.base.cache.support;

/**
 * 缓存名规范化工具：统一 Caffeine 侧 {@code caffeine.cache.<name>.*} 配置名与
 * Spring Cache 注解中 {@code cacheNames} 的匹配口径。
 *
 * <p>Spring 的 {@code CacheManager#getCache(String name)} 对名称<b>不做任何规范化</b>，
 * 直接按原样解析；而本模块的配置键来自 {@code caffeine.cache.<name>}。
 * 若不规范化，{@code @Cacheable(cacheNames = "SampleCache")} 与
 * {@code caffeine.cache.sample-cache.*} 会被当成两个互不相干的缓存：
 * 前者拿不到任何配置、静默套用默认规格（{@code expireInSec=600}、
 * {@code maximumSize=-1} 即不限制容量）。
 *
 * <p>规范化规则：小写 + 去掉分隔符 {@code -} / {@code _} / {@code .}，其余字符保留。
 * 因此 {@code sample-cache}、{@code sample_cache}、{@code SampleCache} 视为同一个缓存，
 * 统一以首次出现的配置名（或首次请求名）作为实际缓存名（保留其原始拼写）。
 */
public final class CacheNameUtils {

    private CacheNameUtils() {
    }

    /**
     * 归一化缓存名，用于建立「配置名 / 请求名」的等价索引。
     *
     * @param name 原始缓存名，可为 {@code null}
     * @return 归一化后的名称；入参为 {@code null} 时返回 {@code null}
     */
    public static String normalize(String name) {
        if (name == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '-' || c == '_' || c == '.') {
                continue;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
