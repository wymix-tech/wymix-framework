# travelsky_dev_decentralization_discovery 去中心化服务发现模块

> 基于 JmDNS（mDNS）的无注册中心服务发现，免去 Eureka / Nacos 等外部依赖。

## 模块说明

`travelsky_dev_decentralization_discovery` 提供去中心化的服务发现能力：通过 JmDNS 在局域网内广播/发现服务实例（`_<namespace>._tcp.local.`），无需独立的注册中心。适用于内网小规模、对运维成本敏感的场景。`DecentralizedDiscoveryConfiguration` 在 `decentralization.discovery.enable=true` 时生效，并依赖 `javax.jmdns.JmDNS` 存在（条件装配）。

## 功能说明

- **mDNS 发现** `DecentralizedDiscoveryConfiguration`：基于 JmDNS 实现服务注册与发现。
- **命名空间隔离** `DecentralizedDiscoveryProperties.namespace`：通过 namespace 划分服务域，类型化为 `_<namespace>._tcp.local.`。
- **服务清单** `service`：声明服务名到实例地址列表的映射。
- **条件装配**：`@ConditionalOnProperty(enable=true)` + `@ConditionalOnClass(JmDNS)`，未启用或缺少依赖时不加载。

## 配置项

配置前缀：`decentralization.discovery`

| 配置项 | 说明 | 默认 |
|--------|------|------|
| `decentralization.discovery.enable` | 是否启用去中心化发现 | false |
| `decentralization.discovery.namespace` | 服务域命名空间（拼为 `_<ns>._tcp.local.`） | — |
| `decentralization.discovery.service.<svc>` | 服务名 → 实例地址列表（List<String>） | — |

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

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_decentralization_discovery</artifactId>
    <version>${revision}</version>
</dependency>
```
