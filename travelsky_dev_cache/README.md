# travelsky_dev_cache 缓存模块

> 多级缓存：Hazelcast 分布式缓存 + Caffeine 本地缓存，按命名空间配置过期策略。

## 模块说明

`travelsky_dev_cache` 在 Spring Cache 抽象之上封装多级缓存能力。提供 Hazelcast 分布式缓存（支持单例/集群/客户端三种部署模式）与 Caffeine 本地缓存（条件装配，依赖存在时自动启用）。通过 `@ConditionalOnClass` 实现真正的按需加载；可通过 `cache.enabled=false`（任意非 `true` 值均可）关闭整个模块的自动装配，两个内嵌子配置将一并失效，默认 `matchIfMissing = true` 即开启。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_cache</artifactId>
    <version>${revision}</version>
</dependency>
```

> Hazelcast 与 Caffeine 在本模块中为 `optional` 依赖，不会随本模块传递引入。需在业务工程中自行引入 `hazelcast-spring`（分布式缓存）或 `caffeine`（本地缓存）依赖，对应缓存能力才会生效。
>
> 本模块继承父 pom：`travelsky_dev_parent3:3.0.0-SNAPSHOT`，Java 17 / Spring Boot 3.5.8。

## 配置项

Hazelcast 前缀：`spring.hazelcast.cache`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.hazelcast.cache.instanceName` | 实例名 | — |
| `spring.hazelcast.cache.mode` | 部署模式：`SINGLETON`/`CLUSTER`/`CLIENT` | `SINGLETON` |
| `spring.hazelcast.cache.tcpIpEnabled` | 是否使用 TCP-IP 发现，`buildConfig()` 直接读取该字段 | `true` |
| `spring.hazelcast.cache.publicAddress` | 对外地址 | `127.0.0.1:5701` |
| `spring.hazelcast.cache.members` | 集群成员列表 | `127.0.0.1` |
| `spring.hazelcast.cache.namespace.<ns>.timeToLiveInSeconds` | 命名空间 TTL | 86400 |
| `spring.hazelcast.cache.namespace.<ns>.evictionPolicy` | 淘汰策略（LRU 等，按 id） | LRU |
| `spring.hazelcast.cache.namespace.<ns>.maxSizePolicy` | 容量策略（PER_NODE 等） | PER_NODE |
| `spring.hazelcast.cache.namespace.<ns>.size` | 最大条目数 | 10000 |
| `spring.hazelcast.cache.namespace.<ns>.keyTimeToLiveInSeconds` | key 级 TTL | 86400 |

Caffeine 前缀：`caffeine`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `caffeine.cache.<name>.maximumSize` | 最大条目数（-1 表示不限制） | -1 |
| `caffeine.cache.<name>.maximumWeight` | 最大权重（-1 表示不限制；**与 `maximumSize` 互斥，同时配置启动报错**；> 0 时必须同时配置 `weigher`） | -1 |
| `caffeine.cache.<name>.weigher` | 权重函数全限定类名（实现 Caffeine `Weigher`，需 public 无参构造；仅允许 `com.travelsky.*` 或 `com.github.benmanes.*` 包内类，其余包启动报可读错误），`maximumWeight > 0` 时必填 | — |
| `caffeine.cache.<name>.initialCapacity` | 初始容量 | 16 |
| `caffeine.cache.<name>.allowNullValues` | 是否允许 null | `true` |
| `caffeine.cache.<name>.expireInSec` | 过期秒数 | 600 |
| `caffeine.cache.<name>.policy` | `AFTER_WRITE` / `AFTER_ACCESS` | `AFTER_WRITE` |
| `caffeine.cache.<name>.refreshAfterWriterInSec` | **已不支持**：刷新需 `LoadingCache` + `CacheLoader` 回源实现，本模块未提供；配置 > 0 时启动报可读错误 | -1 |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `CacheManagerConfiguration` | 自动装配入口，内嵌两个条件装配子配置： |
| ├─ `HazelcastCacheConfiguration` | `@ConditionalOnClass(HazelcastInstance)` 时构建分布式 `CacheManager`（未配置 `default` 命名空间时自动补充 `<app>:default`，TTL 86400s/LRU；自动补充的容量为 `EvictionConfig.DEFAULT_MAX_ENTRY_COUNT * 999 = 9990000`（同时固定为 LRU / PER_NODE），未写入任何命名空间配置项；注意 `CustomHazelcastCacheManager` 对未声明命名空间的 readTimeout 另为硬编码 `3600`s，与该 TTL 无关） |
| └─ `CaffeineCacheConfiguration` | `@ConditionalOnClass(Caffeine)` 时构建本地 `CacheManager` |
| `CustomHazelcastCacheManager` | 自定义 Hazelcast CacheManager，`appPrefix` 由装配方构造器注入（`spring.application.name`，默认 `default`），缓存 key 自动拼接 `<APP>:CACHE:类:方法:参数` |

> 缓存命名空间为 `<app>:<cacheName>` 格式；`appName` 单一来源：`CacheManagerConfiguration` 读取 `spring.application.name`（缺失时默认 `default`），并在装配 `CustomHazelcastCacheManager` 时通过构造器显式传入，未配置应用名时命名空间前缀回退为 `default` 而非 `null`。未在 `spring.hazelcast.cache.namespace` 中声明的名称统一回退到 `default` 命名空间。**两级命名空间说明**：组合缓存下两级 key 并不对齐——Hazelcast 侧为 `<app>:<name>`，Caffeine 侧为原始名（当前为有意设计：Hazelcast 命名空间用于多应用共享集群隔离，Caffeine 为应用内本地缓存无需前缀）；`CompositeCacheManager.getCacheNames()` 聚合的是两级底层命名（Hazelcast 侧含 `<app>:` 前缀）。KeyGenerator 由 `CacheManagerConfiguration` 提供：`<APP大写>:CACHE:<类名>:<方法名>:<参数JSON>`，参数为 null 时拼接 `null`，其余经 JSON 序列化。

## 继承类 / 扩展点

| 扩展点 | 说明 |
|--------|------|
| 标准 Spring Cache 注解 | `@Cacheable` / `@CachePut` / `@CacheEvict` 直接使用 |
| 自定义 KeyGenerator | 直接声明名为 `keyGenerator` 的 Bean 即可覆盖默认实现（默认实现带 `@ConditionalOnMissingBean(KeyGenerator.class)` 保护）；`CacheManagerConfiguration` **不再实现** `CachingConfigurer`，业务侧可自由实现该接口，不会触发多实现冲突 |
| 自定义 CacheManager | 路径一：`cache.enabled=false` 关闭模块自动装配后完全自管；路径二：声明名为 `cacheManager` 的 Bean 并加 `@Primary`（组合缓存带 `@ConditionalOnMissingBean(name = "cacheManager")` 保护，业务 Bean 优先；`@Primary` 用于让 `@EnableCaching` 切面解析唯一主 CacheManager）。注意：仅加 `@Primary` + **其他**自定义 Bean 名不可行，会与自动装配的组合 `cacheManager` 形成双 `@Primary`，启动报 `more than one 'primary' bean found` |
| 依赖引入即生效 | 只加 hazelcast 依赖 → 分布式缓存（`hazelcastCacheManager`）；只加 caffeine 依赖 → 本地缓存（`caffeineCacheManager`）；**两者同时引入可共存**：自动装配组合 `CacheManager`（Bean 名 `cacheManager`，`@Primary`），主 = Hazelcast 分布式、次 = Caffeine 本地，读按主→次，写双写（本地写失败不影响分布式写，仅 warn 日志留痕）；`get(key, valueLoader)` 回源成功后同步回填本地缓存；`getCacheNames()` 聚合两级命名（Hazelcast 侧命名空间为 `<app>:<name>`） |

> 默认 `keyGenerator` Bean 通过内嵌 `CacheCachingConfigurer`（`implements CachingConfigurer`，`@ConditionalOnMissingBean(CachingConfigurer.class)` 保护）接入 Spring Cache 切面——切面只从 `CachingConfigurer` 获取 keyGenerator，不会按名查找容器中的 `keyGenerator` Bean；业务侧实现自己的 `CachingConfigurer` 时该内嵌配置自动让位，不会多实现冲突。同时通过 `@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)` 控制切面优先级。Hazelcast 实例由 `@Bean(destroyMethod = "shutdown")` + `@ConditionalOnMissingBean(HazelcastInstance.class)` 声明（业务侧可自定义 `HazelcastInstance` Bean，不会同名冲突），应用关闭时自动 `shutdown()`。

## 使用示例

```yaml
# application.yml
spring:
  hazelcast:
    cache:
      mode: CLUSTER
      members:
        - 10.0.0.1
        - 10.0.0.2
      namespace:
        user:
          timeToLiveInSeconds: 3600
          size: 5000
caffeine:
  cache:
    dict:
      maximumSize: 1000
      expireInSec: 300
```

```java
@Cacheable(cacheNames = "user", key = "#id")
public User getUser(Long id) { ... }
```
