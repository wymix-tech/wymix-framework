# travelsky_dev_decentralization_discovery 去中心化服务发现模块

> 基于 JmDNS（mDNS）的无注册中心服务发现，免去 Eureka / Nacos 等外部依赖。

## 模块说明

`travelsky_dev_decentralization_discovery` 提供去中心化的服务发现能力：通过 JmDNS 在局域网内广播/发现服务实例（`_<namespace>._tcp.local.`），并内置节点注册、Leader 选举、心跳保活与广播事件机制，无需独立的注册中心。适用于内网小规模、对运维成本敏感的场景。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_decentralization_discovery</artifactId>
    <version>${revision}</version>
</dependency>
```

> ⚠️ JmDNS（`org.jmdns:jmdns`）在本模块中为 `optional` 依赖，业务方需自行显式引入后 mDNS 注册/发现能力才生效；未引入时其余能力（节点注册、选举、心跳、广播）不受影响。本模块仅适用于局域网/内网场景。

## 配置项

配置前缀：`decentralization.discovery`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `decentralization.discovery.enable` | 是否启用去中心化发现（关闭则整个模块不装配；类路径无 JmDNS 时仅 mDNS 注册/发现相关 Bean 不装配，其余能力照常装配） | `false` |
| `decentralization.discovery.namespace` | 服务域命名空间（拼为 `_<ns>._tcp.local.`） | — |
| `decentralization.discovery.service.<svc>` | 服务名 → 实例地址列表（List\<String\>） | — |
| `decentralization.discovery.heartbeat.periodInSeconds` | 心跳周期（秒，`HeartbeatService` 调度间隔；由 `@Value` 读取，非 `DecentralizedDiscoveryProperties` 绑定字段） | `30` |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `DecentralizedDiscoveryConfiguration` | 自动装配入口，继承 `CustomFeignConfiguration`，替换 Feign `Client` 为动态寻址 |
| `DecentralizedDiscoveryConfiguration.JmDnsConfiguration` | 内部配置类，`@ConditionalOnClass(javax.jmdns.JmDNS)` 守卫，仅装配 `JmDNS` 与 `JmDnsService` 两个 Bean（类路径无 JmDNS 时跳过，不影响其余装配） |
| `JmDnsService` | mDNS 注册/发现核心 |
| `NodeService` / `NodeRegistry` / `NodeRefresher` | 节点清单管理与刷新 |
| `ElectionService` / `HeartbeatService` | Leader 选举与心跳 |
| `SelfServiceClusterBroadcast` + `BroadcastListener` | 集群广播与事件处理（`OfflineEventHandler` 等） |
| `DiscoveryFeignClient` / `DynamicUrlTarget` | Feign 客户端动态寻址 |

## 继承类（业务方可实现）

| 类/接口 | 说明 |
|---------|------|
| `EventHandler` | 广播事件处理器 SPI（内置 `OfflineEventHandler`），实现后可处理自定义广播事件 |

## 扩展点（Actuator 端点）

| 端点（`/actuator/<id>`） | 说明 |
|--------------------------|------|
| `nodeRegister` | 节点注册（Write） |
| `election` | 触发选举（Write） |
| `leaderHeartbeat` | Leader 心跳（Write） |
| `broadcast` | 集群广播（Write） |
| `checkLeaderStatus` | 查询 Leader 状态（Read） |

## 使用示例

```yaml
# application.yml
decentralization:
  discovery:
    enable: true
    namespace: travelsky
    service:
      user-service:
        - 10.0.0.1:8080
        - 10.0.0.2:8080
      order-service:
        - 10.0.0.3:8080
```

> ⚠️ mDNS 多播在容器、Kubernetes、跨网段网络环境中通常被禁用或不互通。这类环境建议改用 `core` 模块的 Feign 动态实例（`http.requests.*`）或外部注册中心。
