# decentralization_discovery 去中心化服务发现模块

> 配置驱动的无注册中心服务发现：集群成员由 `decentralization.discovery.service.<svc>` 显式列出，免去 Eureka / Nacos 等外部依赖，也不做任何网络自动发现。

## 模块说明

`decentralization_discovery` 提供去中心化的服务发现能力：集群成员完全由配置指定，实例启动后按配置地址向对端登记（`nodeRegister`），对端同时反向登记本实例；在此基础上内置节点注册、Leader 选举、心跳保活与广播事件机制，无需独立的注册中心。适用于内网小规模、对运维成本敏感的场景。

### 节点来源（不做自动发现）

模块**不做任何网络自动发现**（已移除 JmDNS/mDNS），节点只可能通过以下两种方式进入注册表：

1. 启动时按 `decentralization.discovery.service.<svc>` 配置的地址列表逐一登记；
2. 其它实例主动调用 `POST /actuator/nodeRegister`。

由于第 2 条是双向登记（对端启动时也会向本实例注册），因此**只要任意一端配置正确，双方就能互相感知**。若两端都已启动却因地址不通漏登记，可用 `POST /actuator/nodeRefresh` 重新同步；已登记的地址会跳过，可安全重复调用。

## 依赖引入

```xml
<dependency>
    <groupId>org.wymix.framework</groupId>
    <artifactId>decentralization_discovery</artifactId>
    <version>${revision}</version>
</dependency>
```

> 本模块不再依赖 JmDNS，业务方无需额外引入任何可选依赖。

## 配置项

配置前缀：`decentralization.discovery`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `decentralization.discovery.enable` | 是否启用去中心化发现（关闭则整个模块不装配） | `false` |
| `decentralization.discovery.namespace` | 节点命名空间（写入 `NodeInfo.namespace`，用于区分环境/集群） | — |
| `decentralization.discovery.service.<svc>` | 服务名 → 实例地址列表（List\<String\>，形如 `10.0.0.1:8080`），**集群成员的唯一来源** | — |
| `decentralization.discovery.heartbeat.periodInSeconds` | 心跳周期（秒，`HeartbeatService` 调度间隔；由 `@Value` 读取，非 `DecentralizedDiscoveryProperties` 绑定字段） | `30` |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `DecentralizedDiscoveryConfiguration` | 自动装配入口，继承 `CustomFeignConfiguration`，替换 Feign `Client` 为动态寻址 |
| `NodeService` / `NodeRegistry` / `NodeRefresher` | 节点清单管理与刷新（`NodeService#syncConfiguredNodes` 按配置登记节点） |
| `ElectionService` / `HeartbeatService` | Leader 选举与心跳 |
| `SelfServiceClusterBroadcast` + `BroadcastListener` | 集群广播与事件处理（`OfflineEventHandler` 等） |
| `DiscoveryFeignClient` / `DynamicUrlTarget` | Feign 客户端动态寻址 |

## Leader 选举策略

去中心化发现没有注册中心，每个实例都是**各自计算**得出 Leader，因此选举的核心目标是：各实例独立计算却必须得到同一个结果，否则就会出现多 Leader（脑裂）。

### 触发时机

| 场景 | 入口 | 选举类型 |
|------|------|----------|
| 实例启动完成 | `NodeService#init` | 常规（`up=true`） |
| 收到其它实例注册 | `nodeRegister` 端点 | 常规（`up=true`） |
| 非 Leader 实例探测到 Leader 心跳超时 | `HeartbeatService#heartbeat0` | 重选（`up=false`） |
| 收到 OFFLINE 广播且离线者是 Leader | `OfflineEventHandler` | 重选（`up=false`） |
| 手工触发 | `election` 端点 | 重选（`up=false`） |

### 选优规则

1. 剔除状态为 `DOWN` 的候选；若全部 DOWN 则退回全量候选，宁可产出一个 Leader 也不让集群处于无主状态
2. 常规选举时，现任 Leader 只要仍在候选列表中就**直接连任**（Leader 粘性），避免集群里多一个节点就换 Leader
3. 重选时不看现任，按稳定性得分（`PriorityUtils`，量化到 4 位小数以抹平各实例本地时钟漂移）取最高者
4. **得分相同时按实例名 `serviceName#host:port` 字典序取最小者**——这一级决胜是必需的：各实例的节点列表顺序由注册先后决定并不一致，只比得分会让同分候选在不同实例上胜负相反

### 已知限制

这是无共识协议的各自计算式选举，没有任期（term）与投票阶段。网络分区恢复的瞬间仍可能短暂出现双 Leader；业务若强依赖唯一 Leader，请在上层补充租约校验。

## 继承类（业务方可实现）

| 类/接口 | 说明 |
|---------|------|
| `EventHandler` | 广播事件处理器 SPI（内置 `OfflineEventHandler`），实现后可处理自定义广播事件 |

## 扩展点（Actuator 端点）

| 端点（`/actuator/<id>`） | 说明 |
|--------------------------|------|
| `nodeRegister` | 节点注册（Write） |
| `nodeRefresh` | 按配置重新同步节点，返回 `registered/failed/known/leader`（Write） |
| `nodes` | 查看当前已知节点、自身与 Leader（Read） |
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
    namespace: wymix
    service:
      user-service:
        - 10.0.0.1:8080
        - 10.0.0.2:8080
      order-service:
        - 10.0.0.3:8080
```

### 节点间通信契约（改动前必读）

节点间通过 Feign 调用彼此的 actuator 端点，以下三点任一被破坏都会让注册 / 心跳**静默失败**：

1. **端点参数名必须是 `nodes`**：actuator 的 `@WriteOperation` 是按**参数名**绑定请求体的
   （`{"nodes":...}` 的值赋给名为 `nodes` 的参数），不是把请求体整体反序列化成参数对象。
   调用方发送的是 `HeartbeatDTO`（字段 `nodes`），故参数改名或改成 `HeartbeatDTO` 类型都会报
   `MissingParametersException`。
2. **端点返回类型必须与 `DiscoveryFeignClient` 的声明一致**：`nodeRegister` 返回 `NodeInfo` 对象；
   若返回 JSON 字符串，会被按 `application/json` 二次编码成带引号的字符串，调用方反序列化失败。
3. **actuator 端点不能被统一响应包装**：`web` 模块的 `ControllerResponseAdvice` 会按请求路径排除
   `management.endpoints.web.base-path`（默认 `/actuator`）下的请求。若该排除失效，响应会被套上
   `{"statusCode":..,"payload":..}` 外壳，节点间调用将全部反序列化失败。

### Spring Cloud 版本

模块依赖 Spring Cloud OpenFeign 4.x 的 `FeignClientFactory`（4.1 起取代了 `FeignContext`；
`setDecode404` 也更名为 `setDismiss404`）。版本由根 POM 的 `spring-cloud-dependencies` BOM
统一管理，需与 Boot 版本匹配（Boot 3.5.x → Spring Cloud 2025.0.x）。

配置里的地址必须是实例之间可直接访问的 `host:port`（`DynamicUrlTarget` 会拼成 `http://host:port`）。
跨网段、容器与 Kubernetes 环境只要地址可达即可正常工作——这正是改用配置而非 mDNS 多播的原因。

新增 / 剔除实例时需同步修改各实例的 `service.<svc>` 配置并重启（或用 `POST /actuator/nodeRefresh` 重新同步）。
