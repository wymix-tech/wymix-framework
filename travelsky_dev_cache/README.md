# travelsky_dev_cache 缓存模块

> 多级缓存：Hazelcast 分布式缓存 + Caffeine 本地缓存，按命名空间配置过期策略。

## 模块说明

`travelsky_dev_cache` 在 Spring Cache 抽象之上封装多级缓存能力。提供 Hazelcast 分布式缓存（支持单例/集群/客户端三种部署模式）与 Caffeine 本地缓存（条件装配，依赖存在时自动启用）。通过 `@ConditionalOnClass` 实现真正的按需加载。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_cache</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

Hazelcast 前缀：`spring.hazelcast.cache`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.hazelcast.cache.instanceName` | 实例名 | — |
| `spring.hazelcast.cache.mode` | 部署模式：`SINGLETON`/`CLUSTER`/`CLIENT` | `SINGLETON` |
| `spring.hazelcast.cache.tcpIpEnabled` | 是否使用 TCP-IP 发现 | `true` |
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
| `caffeine.cache.<name>.maximumSize` | 最大条目数 | 999 |
| `caffeine.cache.<name>.maximumWeight` | 最大权重（-1 表示不限制） | -1 |
| `caffeine.cache.<name>.initialCapacity` | 初始容量 | 16 |
| `caffeine.cache.<name>.allowNullValues` | 是否允许 null | `true` |
| `caffeine.cache.<name>.expireInSec` | 过期秒数 | 600 |
| `caffeine.cache.<name>.policy` | `AFTER_WRITE` / `AFTER_ACCESS` | `AFTER_WRITE` |
| `caffeine.cache.<name>.refreshAfterWriterInSec` | 写入后刷新秒数（-1 关闭） | -1 |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `CacheManagerConfiguration` | 自动装配入口，内嵌两个条件装配子配置： |
| ├─ `HazelcastCacheConfiguration` | `@ConditionalOnClass(HazelcastInstance)` 时构建分布式 `CacheManager`（未配置 `default` 命名空间时自动补充 `<app>:default`，TTL 86400s/LRU） |
| └─ `CaffeineCacheConfiguration` | `@ConditionalOnClass(Caffeine)` 时构建本地 `CacheManager` |
| `CustomHazelcastCacheManager` | 自定义 Hazelcast CacheManager，缓存 key 自动拼接 `<APP>:CACHE:类:方法:参数` |

## 继承类 / 扩展点

| 扩展点 | 说明 |
|--------|------|
| 标准 Spring Cache 注解 | `@Cacheable` / `@CachePut` / `@CacheEvict` 直接使用 |
| `CachingConfigurer` | Spring 标准扩展接口，可自定义 key 生成等 |
| 依赖引入即生效 | 加 hazelcast 依赖 → 分布式缓存；加 caffeine 依赖 → 本地缓存；两者可共存 |

> ⚠️ `CacheManagerConfiguration` 继承了 Spring 已废弃的 `CachingConfigurerSupport`，在 Spring Boot 3.x 下建议改为实现 `CachingConfigurer` 接口。

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
