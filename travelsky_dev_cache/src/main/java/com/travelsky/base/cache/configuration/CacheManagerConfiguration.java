package com.travelsky.base.cache.configuration;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.travelsky.base.cache.properties.CaffeineCacheProperties;
import com.travelsky.base.cache.support.CacheNameUtils;
import com.travelsky.base.utils.json.JsonUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存自动装配。
 *
 * <p>本模块职责收敛为：<b>为业务工程引入 Caffeine 本地缓存，并托管其配置装配</b>。
 * 不提供分布式缓存，也不做多级/组合缓存——需要分布式缓存请自行引入实现并在业务侧
 * 声明 {@code CacheManager}（组合语义与读写策略属于业务决策，不由框架预设）。
 *
 * <p>装配条件：
 * <ul>
 *   <li>{@code cache.enabled} 非 {@code false}（默认开启）；</li>
 *   <li>classpath 存在 Caffeine；</li>
 *   <li>业务侧未自行声明 {@code CacheManager}（声明即整体让位）。</li>
 * </ul>
 *
 * <p>缓存规格来自 {@code caffeine.cache.<name>.*}；仅声明规格的缓存会被预注册，
 * 未声明的缓存名按默认规格懒建（{@code CaffeineCacheManager} 的 missing-cache 机制）。
 * 缓存名匹配统一走 {@link CacheNameUtils#normalize(String)}，因此
 * {@code @Cacheable(cacheNames = "SampleCache")} 能命中 {@code caffeine.cache.sample-cache.*}。
 */
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
@AutoConfiguration
@ConditionalOnClass(Caffeine.class)
@ConditionalOnProperty(prefix = "cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CaffeineCacheProperties.class)
@Configuration(proxyBeanMethods = false)
public class CacheManagerConfiguration {

    /**
     * 默认应用名：{@code spring.application.name} 未配置时用作 keyGenerator 前缀（单一来源）。
     */
    public static final String DEFAULT_APP_NAME = "default";

    private static final String SPLITTER = ":";

    @Value("${spring.application.name:" + DEFAULT_APP_NAME + "}")
    private String appName;

    /**
     * 默认 KeyGenerator，格式 {@code <APP大写>:CACHE:<类名>:<方法名>:<参数JSON>}。
     *
     * <p>注意两点：
     * <ul>
     *   <li>Spring Cache 切面只从 {@link CachingConfigurer} 获取默认 keyGenerator，
     *       <b>不会</b>按名查找容器中的 {@code keyGenerator} Bean（见 {@code CacheInterceptor}）；
     *       本 Bean 由 {@link CacheCachingConfigurer} 接入切面；</li>
     *   <li>业务侧声明同名 {@code keyGenerator} Bean 即可覆盖（带
     *       {@code @ConditionalOnMissingBean(KeyGenerator.class)} 保护）。</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean(KeyGenerator.class)
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(appName.toUpperCase()).append(SPLITTER);
            sb.append("CACHE").append(SPLITTER);
            sb.append(target.getClass().getSimpleName()).append(SPLITTER);
            sb.append(method.getName()).append(SPLITTER);
            for (Object param : params) {
                sb.append(param == null ? "null" : JsonUtils.entityToJson(param));
            }
            return sb.toString();
        };
    }

    /**
     * 本地缓存 CacheManager：按 {@code caffeine.cache.<name>} 分别应用规格，
     * 未声明的缓存名按默认规格懒建。
     */
    @Bean
    @ConditionalOnMissingBean(name = "cacheManager")
    public CacheManager cacheManager(CaffeineCacheProperties properties) {
        return new SpecCacheManager(properties);
    }

    static Caffeine<Object, Object> buildCaffeine(CaffeineCacheProperties.CaffeineConfig config) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder();
        caffeine.initialCapacity(config.getInitialCapacity());
        if (config.getMaximumSize() > 0 && config.getMaximumWeight() > 0) {
            throw new IllegalStateException(
                    "caffeine.cache.<name>.maximumSize 与 maximumWeight 互斥，只能配置其中一个（"
                            + "maximumSize=" + config.getMaximumSize()
                            + ", maximumWeight=" + config.getMaximumWeight() + "）");
        }
        if (config.getMaximumSize() > 0) {
            caffeine.maximumSize(config.getMaximumSize());
        }
        if (config.getMaximumWeight() > 0) {
            if (config.getWeigher() == null || config.getWeigher().isBlank()) {
                throw new IllegalStateException(
                        "配置 caffeine.cache.<name>.maximumWeight 时必须同时提供 weigher（"
                                + "maximumWeight=" + config.getMaximumWeight() + "）");
            }
            caffeine.maximumWeight(config.getMaximumWeight())
                    .weigher(newWeightedWeigher(config.getWeigher()));
        }
        if (config.getPolicy() == CaffeineCacheProperties.Policy.AFTER_WRITE) {
            caffeine.expireAfterWrite(config.getExpireInSec(), TimeUnit.SECONDS);
        } else {
            caffeine.expireAfterAccess(config.getExpireInSec(), TimeUnit.SECONDS);
        }
        if (config.getRefreshAfterWriterInSec() > 0) {
            throw new IllegalStateException(
                    "caffeine.cache.<name>.refreshAfterWriterInSec 需要配合 CacheLoader 构建 LoadingCache，"
                            + "当前模块未提供回源实现，请勿配置该选项（"
                            + "refreshAfterWriterInSec=" + config.getRefreshAfterWriterInSec() + "）");
        }
        caffeine.evictionListener(new RemovalListener<Object, Object>() {
            @Override
            public void onRemoval(@Nullable Object key, @Nullable Object value, @NonNull RemovalCause cause) {
                // 保留扩展点：Caffeine 淘汰事件的默认实现为空，业务可在此接入监控
            }
        });
        caffeine.removalListener(new RemovalListener<Object, Object>() {
            @Override
            public void onRemoval(@Nullable Object key, @Nullable Object value, @NonNull RemovalCause cause) {
                // 同上
            }
        });
        return caffeine.recordStats();
    }

    /**
     * 按全限定类名实例化 Caffeine 权重函数，类型不匹配或实例化失败时抛出可读异常。
     */
    @SuppressWarnings("unchecked")
    private static com.github.benmanes.caffeine.cache.Weigher<Object, Object> newWeightedWeigher(String weigherClass) {
        String trimmed = weigherClass.trim();
        // 类名来自配置文件，限制包前缀白名单，避免配置被污染时加载并实例化 classpath 上任意类
        if (!trimmed.startsWith("com.travelsky.") && !trimmed.startsWith("com.github.benmanes.")) {
            throw new IllegalStateException(
                    "weigher 不在允许的包范围内（仅支持 com.travelsky.* 或 com.github.benmanes.*）");
        }
        Object instance;
        try {
            Class<?> clazz = Class.forName(trimmed);
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException("无法实例化 weigher（需提供 public 无参构造，"
                    + "且类名需在允许的包范围内）", ex);
        }
        if (!(instance instanceof com.github.benmanes.caffeine.cache.Weigher)) {
            throw new IllegalStateException("weigher 必须实现 com.github.benmanes.caffeine.cache.Weigher"
                    + "（实际类型: " + instance.getClass().getName() + "）");
        }
        return (com.github.benmanes.caffeine.cache.Weigher<Object, Object>) instance;
    }

    /**
     * 在 {@link CaffeineCacheManager} 之上补齐 Caffeine 与 Spring 都无法直接表达的两件事：
     * <ol>
     *   <li><b>逐缓存规格 + 未声明缓存懒建</b>：{@code caffeine.cache.<name>} 中声明的缓存
     *       按各自规格创建，未声明的缓存名首次请求时按默认规格懒建。不直接使用
     *       {@link CaffeineCacheManager#setCacheNames(Collection)}，因为它会把缓存名锁成
     *       静态清单（未声明名称从此不可用），且每次调用都会重建缓存实例、丢掉已有数据。
     *       本实现自行持有缓存实例，{@code getCache} 命中内存表即返回，天然满足
     *       「规格生效 + 未声明可懒建 + 内容不丢」；</li>
     *   <li><b>缓存名归一化</b>：Spring 的 {@code getCache(String)} 对名称零规范化，
     *       {@code @Cacheable(cacheNames = "SampleCache")} 与
     *       {@code caffeine.cache.sample-cache.*} 若不归一化就是两个互不相干的缓存
     *       （前者静默套用默认规格、配置形同失效）。这里按
     *       {@link CacheNameUtils#normalize(String)} 建立「请求名 → 对外名」别名，
     *       保证两者命中同一实例，且不派生多余缓存名。</li>
     * </ol>
     *
     * <p>与 {@link CaffeineCacheManager} 的关键差异在于缓存实例的归属：本类自己
     * {@code build()} 原生 Caffeine Cache 并包成 {@code CaffeineCache}，全程复用同一实例，
     * 因此不存在「重建适配器导致数据丢失」的窗口。
     */
    static class SpecCacheManager implements CacheManager {

        private final CaffeineCacheProperties properties;

        /** 归一化名 -> 对外缓存名（配置名优先，其次首次请求名，保留原始拼写）。 */
        private final Map<String, String> aliases = new java.util.concurrent.ConcurrentHashMap<>();

        /** 已创建的缓存（对外名 -> 缓存实例），全部由本类构建与持有，跨模式不重建、内容不丢。 */
        private final Map<String, Cache> caches = new java.util.concurrent.ConcurrentHashMap<>();

        /** 在 {@code caffeine.cache} 中声明了独立规格的缓存名（对外名）。 */
        private final java.util.Set<String> declaredNames = java.util.concurrent.ConcurrentHashMap.newKeySet();

        /** 已声明缓存的规格；未声明缓存走 {@code caffeine.cache} 之外的默认规格。 */
        private final Map<String, CaffeineCacheProperties.CaffeineConfig> specs = new java.util.concurrent.ConcurrentHashMap<>();

        private final CaffeineCacheProperties.CaffeineConfig defaultSpec;

        SpecCacheManager(CaffeineCacheProperties properties) {
            this.properties = properties;
            this.defaultSpec = new CaffeineCacheProperties.CaffeineConfig();
            // 未声明规格的缓存名使用 caffeine.defaultMaximumSize 作为容量兜底
            // （默认 10000），避免出现无界本地缓存；显式设为 -1 即恢复不限容量
            this.defaultSpec.setMaximumSize(properties.getDefaultMaximumSize());
            Map<String, CaffeineCacheProperties.CaffeineConfig> configured = properties.getCache();
            if (configured != null) {
                configured.forEach((name, config) -> {
                    declaredNames.add(name);
                    specs.put(name, config);
                    aliases.putIfAbsent(CacheNameUtils.normalize(name), name);
                });
            }
        }

        @Override
        public Cache getCache(String name) {
            if (name == null) {
                return null;
            }
            String normalized = CacheNameUtils.normalize(name);
            // 「名解析」与「实例创建」两步都原子化：
            //  1. aliases.computeIfAbsent 先抢占归一化名 -> 实际名的映射，
            //     并发下多个拼写（SampleCache / sample-cache / samplecache）只会有一个
            //     成为实际名，其余请求解析到同一实例，不会各自建实例；
            //  2. caches.computeIfAbsent 包住 buildCache，buildCache 不会返回 null，
            //     可安全使用（并发不同实际名各自建一次，同名只建一次）。
            // 若把别名登记放在建实例之后，别名登记前存在窗口会派生重复缓存名，
            // getCacheNames() 会同时泄漏多个拼写。
            String actual = aliases.computeIfAbsent(normalized, key -> name);
            return caches.computeIfAbsent(actual, this::buildCache);
        }

        private Cache buildCache(String name) {
            // 与 CacheManagerConfiguration#buildCaffeine 的前置校验保持一致：
            // 规格非法时在首次请求该缓存时报可读异常，而不是留到 Caffeine 内部
            CaffeineCacheProperties.CaffeineConfig config = specs.getOrDefault(name, defaultSpec);
            boolean async = properties.isAsyncCacheMode();
            // allowNullValues 来自 caffeine.cache.<name>.allowNullValues，必须真实传入
            // CaffeineCache 构造器；写死 true 会让该配置项成为死配置（配了 false 仍存 null，
            // 与 README 声明不符）
            boolean allowNullValues = config.isAllowNullValues();
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = buildCaffeine(config).build();
            if (async) {
                // 异步模式：包装成 AsyncCache 形态的 CaffeineCache，retrieve 才可用。
                // 两种形态共用同一个原生 Cache 实例，因此切换 caffeine.asyncCacheMode
                // 只影响新建缓存，已建缓存的内容不会丢失
                AsyncCache<Object, Object> asyncCache = new SameCacheAsyncAdapter(nativeCache);
                return new org.springframework.cache.caffeine.CaffeineCache(name, asyncCache, allowNullValues);
            }
            return new org.springframework.cache.caffeine.CaffeineCache(name, nativeCache, allowNullValues);
        }

        @Override
        public Collection<String> getCacheNames() {
            // 已声明的缓存名（即使尚未被请求，也属于本模块对外承诺的缓存集合）
            // 加上已请求过的未声明缓存名，名称集合单调递增且稳定。
            // 与 getCache() 一致地只暴露「实际缓存名」（aliases 的 value 集合），
            // 不暴露归一化中间名 —— 别名登记先于实例创建完成，两者不会出现
            // 「有别名但无实例」的空窗，故此处与 caches.keySet() 等价且更语义明确。
            java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
            names.addAll(declaredNames);
            for (String actual : aliases.values()) {
                if (caches.containsKey(actual)) {
                    names.add(actual);
                }
            }
            return java.util.Collections.unmodifiableSet(names);
        }
    }

    /**
     * 把已有的原生 {@link com.github.benmanes.caffeine.cache.Cache} 适配为
     * {@link AsyncCache}，使 {@code CaffeineCache} 具备 {@code retrieve} 能力，
     * 同时<b>复用同一个底层实例</b>——避免「切换到异步模式」时数据被换库丢失。
     *
     * <p>Caffeine 未提供 {@code Cache → AsyncCache} 的公开转换，故此处实现薄适配：
     * {@code retrieve} 相关的三个方法提供完整异步语义，其余 {@link ConcurrentMap}
     * 能力（含 {@code keySet()}）全部委托原生实例，保证 {@code put} / {@code get} /
     * {@code invalidate} 等同步操作与异步视图看到的是同一份数据。
     */
    private static final class SameCacheAsyncAdapter implements AsyncCache<Object, Object> {

        /** 直接在当前线程执行的 Executor：本适配器不引入额外线程池。 */
        private static final Executor DIRECT_EXECUTOR = Runnable::run;

        private final com.github.benmanes.caffeine.cache.Cache<Object, Object> delegate;

        SameCacheAsyncAdapter(com.github.benmanes.caffeine.cache.Cache<Object, Object> delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletableFuture<Object> getIfPresent(Object key) {
            return CompletableFuture.completedFuture(delegate.getIfPresent(key));
        }

        @Override
        public CompletableFuture<Object> get(Object key, java.util.function.Function<? super Object, ? extends Object> mappingFunction) {
            Object present = delegate.getIfPresent(key);
            if (present != null) {
                return CompletableFuture.completedFuture(present);
            }
            Object value = mappingFunction.apply(key);
            if (value != null) {
                delegate.put(key, value);
            }
            return CompletableFuture.completedFuture(value);
        }

        @Override
        public CompletableFuture<Object> get(Object key,
                java.util.function.BiFunction<? super Object, ? super Executor,
                        ? extends CompletableFuture<? extends Object>> mappingFunction) {
            Object present = delegate.getIfPresent(key);
            if (present != null) {
                return CompletableFuture.completedFuture(present);
            }
            CompletableFuture<? extends Object> future = mappingFunction.apply(key, DIRECT_EXECUTOR);
            if (future == null) {
                return CompletableFuture.completedFuture(null);
            }
            return future.thenApply(value -> {
                if (value != null) {
                    delegate.put(key, value);
                }
                return (Object) value;
            });
        }

        @Override
        public CompletableFuture<java.util.Map<Object, Object>> getAll(Iterable<? extends Object> keys,
                java.util.function.Function<? super java.util.Set<? extends Object>, ? extends java.util.Map<? extends Object, ? extends Object>> mappingFunction) {
            java.util.Map<Object, Object> result = new java.util.LinkedHashMap<>();
            java.util.Set<Object> missing = new java.util.LinkedHashSet<>();
            for (Object key : keys) {
                Object value = delegate.getIfPresent(key);
                if (value == null) {
                    missing.add(key);
                } else {
                    result.put(key, value);
                }
            }
            if (missing.isEmpty()) {
                return CompletableFuture.completedFuture(result);
            }
            java.util.Map<? extends Object, ? extends Object> loaded = mappingFunction.apply(missing);
            if (loaded != null) {
                loaded.forEach((k, v) -> {
                    if (v != null) {
                        delegate.put(k, v);
                        result.put(k, v);
                    }
                });
            }
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public CompletableFuture<java.util.Map<Object, Object>> getAll(Iterable<? extends Object> keys,
                java.util.function.BiFunction<? super java.util.Set<? extends Object>, ? super Executor,
                        ? extends CompletableFuture<? extends java.util.Map<? extends Object, ? extends Object>>> mappingFunction) {
            java.util.Map<Object, Object> result = new java.util.LinkedHashMap<>();
            java.util.Set<Object> missing = new java.util.LinkedHashSet<>();
            for (Object key : keys) {
                Object value = delegate.getIfPresent(key);
                if (value == null) {
                    missing.add(key);
                } else {
                    result.put(key, value);
                }
            }
            if (missing.isEmpty()) {
                return CompletableFuture.completedFuture(result);
            }
            CompletableFuture<? extends java.util.Map<? extends Object, ? extends Object>> future =
                    mappingFunction.apply(missing, DIRECT_EXECUTOR);
            if (future == null) {
                return CompletableFuture.completedFuture(result);
            }
            return future.thenApply(loaded -> {
                if (loaded != null) {
                    loaded.forEach((k, v) -> {
                        if (v != null) {
                            delegate.put(k, v);
                            result.put(k, v);
                        }
                    });
                }
                return result;
            });
        }

        @Override
        public void put(Object key, CompletableFuture<? extends Object> value) {
            if (value == null) {
                return;
            }
            value.thenAccept(v -> {
                if (v != null) {
                    delegate.put(key, v);
                }
            });
        }

        @Override
        public java.util.concurrent.ConcurrentMap<Object, CompletableFuture<Object>> asMap() {
            throw new UnsupportedOperationException("SameCacheAsyncAdapter 未提供 asMap() 视图");
        }

        @Override
        public com.github.benmanes.caffeine.cache.Cache<Object, Object> synchronous() {
            return delegate;
        }
    }

    /**
     * 将默认 {@code keyGenerator} 接入 Spring Cache 切面。
     *
     * <p>{@code CacheInterceptor} 的 keyGenerator 仅来源于 {@link CachingConfigurer}
     * （见 {@code AbstractCachingConfiguration#useCachingConfigurer}），没有
     * {@code CachingConfigurer} 时会静默回退到 {@code SimpleKeyGenerator}，
     * 导致 {@code <APP>:CACHE:...} 格式的 key 失效。
     *
     * <p><b>与业务侧的协同</b>：Spring 启动期由
     * {@code AbstractCachingConfiguration#setConfigurers} 收集容器内全部
     * {@link CachingConfigurer}，数量大于 1 时直接抛
     * {@code IllegalStateException: 2 implementations of CachingConfigurer were found}。
     * {@code @ConditionalOnMissingBean} 在此处不可靠——业务侧的 {@code CachingConfigurer}
     * 若来自另一批独立配置/自动装配，本配置评估条件时对方可能尚未注册，条件会误判。
     * 因此本配置<b>无条件注册</b>，与业务侧自定义 {@code CachingConfigurer} 是
     * 「二选一」关系：业务侧如需接管，请先整体关闭本模块自动装配
     * （{@code cache.enabled=false}）再声明自己的实现。
     */
    @Configuration(proxyBeanMethods = false)
    public static class CacheCachingConfigurer implements CachingConfigurer {

        private final KeyGenerator keyGenerator;

        public CacheCachingConfigurer(KeyGenerator keyGenerator) {
            this.keyGenerator = keyGenerator;
        }

        @Override
        public KeyGenerator keyGenerator() {
            return keyGenerator;
        }

        /**
         * 返回 {@code null}：不接管 CacheManager 解析，由 Spring 按类型从容器解析
         * （模块装配的 {@code cacheManager}，或业务自行声明的 {@code CacheManager}）。
         */
        @Override
        public CacheManager cacheManager() {
            return null;
        }

        /**
         * 返回 {@code null}：不接管 CacheResolver，走 Spring 默认实现。
         */
        @Override
        public CacheResolver cacheResolver() {
            return null;
        }
    }
}
