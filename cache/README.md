# cache 缓存模块

> Caffeine 本地缓存：引入依赖即获得配置化的进程内缓存能力。

## 模块说明

`cache` 的职责是**为开发人员引入 Caffeine 本地缓存并托管其配置装配**：

- 基于 Spring Cache 抽象，业务侧直接使用 `@Cacheable` / `@CachePut` / `@CacheEvict`；
- 通过 `@EnableCaching` 开启缓存，并提供统一的默认 `KeyGenerator`；
- 缓存规格集中在 `caffeine.cache.<name>.*` 声明，未声明的缓存名按默认规格懒建。

本模块**不提供分布式缓存，也不做多级/组合缓存**：需要分布式缓存（如 Hazelcast/Redis）时，
请由业务工程自行引入实现并在业务侧声明 `CacheManager`——主次顺序、写入策略、回源语义
属于业务决策，不由框架预设，否则必然出现「配了就崩」或「读到的值与预期不符」这类隐式行为。

可通过 `cache.enabled=false`（任意非 `true` 值均可）关闭整个模块的自动装配，默认
`matchIfMissing = true` 即开启。

## 依赖引入

```xml
<dependency>
    <groupId>org.wymix.framework</groupId>
    <artifactId>cache</artifactId>
    <version>${revision}</version>
</dependency>
```

> `caffeine` 在本模块中为 `optional` 依赖，不会随本模块传递引入，需业务工程自行引入，缓存能力才会生效。
> 本模块继承父 pom：`dev_parent:3.0.0-SNAPSHOT`，Java 17 / Spring Boot 3.5.8。

## 配置项

前缀：`caffeine`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `caffeine.asyncCacheMode` | 是否启用 Caffeine 异步缓存模式（`AsyncCache`）。`retrieve()` / `retrieve(key, Supplier)` 仅在开启时可用；关闭后这两个方法会抛 `IllegalStateException: No Caffeine AsyncCache available`（普通读写不受影响） | `true` |
| `caffeine.defaultMaximumSize` | **未在 `caffeine.cache` 中声明规格的缓存名**的默认最大条目数（-1 表示不限制），避免无界本地缓存 | 10000 |
| `caffeine.cache.<name>.maximumSize` | 最大条目数（-1 表示不限制） | -1 |
| `caffeine.cache.<name>.maximumWeight` | 最大权重（-1 表示不限制；**与 `maximumSize` 互斥，同时配置启动报错**；> 0 时必须同时配置 `weigher`） | -1 |
| `caffeine.cache.<name>.weigher` | 权重函数全限定类名（实现 Caffeine `Weigher`，需 public 无参构造；允许业务包，类型须实现 Weigher，校验通过后才实例化），`maximumWeight > 0` 时必填 | — |
| `caffeine.cache.<name>.initialCapacity` | 初始容量 | 16 |
| `caffeine.cache.<name>.allowNullValues` | 是否允许 null（`false` 时 `put(k, null)` 抛 `IllegalArgumentException`） | `true` |
| `caffeine.cache.<name>.expireInSec` | 过期秒数 | 600 |
| `caffeine.cache.<name>.policy` | `AFTER_WRITE` / `AFTER_ACCESS` | `AFTER_WRITE` |
| `caffeine.cache.<name>.refreshAfterWriterInSec` | **已不支持**：刷新需 `LoadingCache` + `CacheLoader` 回源实现，本模块未提供；配置 > 0 时启动报可读错误 | -1 |

配置项说明补充：

- `caffeine.asyncCacheMode`（默认 `true`）。开启时缓存以 Caffeine `AsyncCache` 形态构建，
  普通读写行为不变，额外获得 `Cache#retrieve` 能力（Spring 6.1 的响应式/异步缓存路径依赖它）。
  代价是该模式下不能设置 `CacheLoader`（Caffeine 的 `AsyncCacheLoader` 与 `CacheLoader` 互斥），
  因此本模块不支持 `refreshAfterWriterInSec`。
- `maximumSize` 与 `maximumWeight` 互斥，两者同时 `> 0` 时启动即抛可读的 `IllegalStateException`，
  不会落到 Caffeine 内部异常；`maximumWeight` 单独使用时必须提供 `weigher`。
- `weigher` 来自可信配置；允许业务包实现，先校验 Weigher 类型再调用构造器，拒绝任意非权重函数类型。
- 未在 `caffeine.cache` 中声明规格的缓存名，默认规格的容量取自 `caffeine.defaultMaximumSize`
  （默认 `10000`）。这样业务只写 `@Cacheable(cacheNames = "xxx")` 而未声明规格时，
  也不会开出无界本地缓存（TTL 窗口内 key 基数大时会无上限增长，是 OOM 风险点）。
  如需默认不限制容量，显式设 `caffeine.defaultMaximumSize=-1`，并自行保证为每个缓存名声明规格。
- `allowNullValues` 会真实传入 `CaffeineCache` 构造器：设为 `false` 时该缓存拒绝写入 `null`
  （抛 `IllegalArgumentException`），默认 `true` 保持「允许缓存 null、读取用 `ValueWrapper` 包裹」的行为。

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `CacheManagerConfiguration` | 自动装配入口：`@ConditionalOnClass(Caffeine)` + `@ConditionalOnProperty(cache.enabled)`，注册 `cacheManager` 与默认 `keyGenerator` |
| `CacheManagerConfiguration.SpecCacheManager` | 包装 `CaffeineCacheManager`：按 `caffeine.cache.<name>` 应用逐缓存规格，并对缓存名做归一化 |
| `CacheManagerConfiguration.CacheCachingConfigurer` | 将默认 `keyGenerator` 接入 Spring Cache 切面（见下） |
| `CaffeineCacheProperties` | `caffeine.*` 配置绑定 |
| `CacheNameUtils` | 缓存名归一化工具（见下） |

装配条件：`cache.enabled` 非 `false`（默认开启）、classpath 存在 Caffeine、业务侧未声明
名为 `cacheManager` 的 Bean。

缓存名归一化：Spring 的 `CacheManager#getCache(String)` 对名称**不做任何规范化**。本模块按
「小写 + 去掉 `-` / `_` / `.`」建立「请求名 → 声明名」别名，因此
`@Cacheable(cacheNames = "SampleCache")`、`sample-cache`、`sample_cache` 会命中同一缓存实例，
并以 `caffeine.cache.sample-cache.*` 中声明的规格生效。未在 `caffeine.cache` 中声明的缓存名
首次请求时按默认规格懒建，以首次请求名作为实际缓存名。归一化名的解析与缓存实例的创建
在同一 `ConcurrentHashMap` 原子操作内完成，因此并发首访不同拼写不会各建实例，
`getCacheNames()` 也不会同时出现多个拼写。

KeyGenerator 由 `CacheManagerConfiguration` 提供：`<APP大写>:CACHE:<类名>:<方法名>:<参数JSON>`，
参数为 `null` 时拼接 `null`，其余经 JSON 序列化；`<APP>` 取 `spring.application.name`，
缺失时回退为 `default`（单一来源常量 `CacheManagerConfiguration.DEFAULT_APP_NAME`）。

## 继承类 / 扩展点

| 扩展点 | 说明 |
|--------|------|
| 标准 Spring Cache 注解 | `@Cacheable` / `@CachePut` / `@CacheEvict` 直接使用 |
| 自定义 KeyGenerator | 直接声明名为 `keyGenerator` 的 Bean 即可覆盖默认实现（默认实现带 `@ConditionalOnMissingBean(KeyGenerator.class)` 保护），模块的 `CacheCachingConfigurer` 会自动引用它 |
| 自定义 CacheManager | 路径一：声明**名为 `cacheManager`** 的 Bean（模块的 `cacheManager` 带 `@ConditionalOnMissingBean(name = "cacheManager")` 保护，业务 Bean 优先）；路径二：`cache.enabled=false` 关闭模块自动装配后完全自管。注意：业务 Bean 若用**其他名称**，模块的 `cacheManager` 仍会注册，两者共存需业务侧自行用 `@Primary` + 自定义 `CachingConfigurer` 决定解析结果 |
| 自定义 `CachingConfigurer` | 模块固定注册内嵌 `CacheCachingConfigurer`，与业务侧实现是**二选一**关系：业务侧须先整体关闭模块自动装配（`cache.enabled=false`）再声明自己的 `CachingConfigurer`，否则容器内出现 2 个实现，启动报 `2 implementations of CachingConfigurer were found` |
| 业务自行引入分布式缓存 | 本模块不再提供分布式缓存装配。业务侧引入 Hazelcast/Redis 等实现并声明自己的 `CacheManager` + `CachingConfigurer`（或组合它们）即可，本模块不参与 |

> 默认 `keyGenerator` 通过内嵌 `CacheCachingConfigurer`（`implements CachingConfigurer`，
> **无条件注册**）接入切面——切面只从 `CachingConfigurer` 获取 keyGenerator，不会按名查找
> 容器中的 `keyGenerator` Bean。`@ConditionalOnMissingBean` 在此处不可靠（业务侧实现若来自
> 另一批独立配置，条件评估时对方可能尚未注册），故不承诺「自动让位」，而是明确二选一。
> 该内嵌配置的 `cacheManager()` / `cacheResolver()` 均返回 `null`，不接管解析，由 Spring 从容器
> 按类型解析；同时通过 `@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)` 控制切面优先级。

## 使用示例

```yaml
# application.yml
cache:
  enabled: true
caffeine:
  asyncCacheMode: true
  cache:
    user:
      maximumSize: 5000
      expireInSec: 3600
      policy: AFTER_WRITE
    dict:
      maximumSize: 1000
      expireInSec: 300
```

```java
@Cacheable(cacheNames = "user", key = "#id")
public User getUser(Long id) { ... }
```

> `cacheNames = "user"` 与 `caffeine.cache.user.*` 名称一致（归一化后大小写与分隔符不敏感）。
> 未在 yaml 中声明规格的缓存名同样可用，按默认规格（`expireInSec=600`、
> `maximumSize=caffeine.defaultMaximumSize` 默认 `10000`）懒建。

## 降级说明（历史变更）

本模块此前同时装配 Hazelcast 分布式缓存与 Caffeine 本地缓存，并试图在两者同时存在时提供
「组合缓存」语义。该组合语义涉及主次顺序、写入策略、命名空间对齐、异常隔离等大量隐式约定，
评审中反复出现读后不一致、本地缓存实际失效、`retrieve()` 形态不匹配等问题，已整体移除：

- 删除 `HazelcastCacheProperties`、`CustomHazelcastCacheManager`、`CompositeCacheManager`；
- 删除 `hazelcast-spring` 依赖与 `spring.hazelcast.cache.*` 全部配置项；
- 模块收敛为「引入 Caffeine 本地缓存 + 托管其配置」这一件事。

需要分布式缓存或多个 CacheManager 的组合语义时，请由业务侧显式装配。
