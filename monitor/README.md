# monitor 监控模块

> 基于 Actuator + Micrometer 的应用可观测性：指标暴露与启动事件采集。

## 模块说明

`monitor` 在 Spring Boot Actuator 与 Micrometer 之上，暴露 Actuator 标准指标（JVM、HTTP 等），并通过 `MonitorApplicationStartedEventListener` 在环境准备阶段（`ApplicationEnvironmentPreparedEvent`）注入 Actuator/Prometheus 默认配置。该监听器由 `META-INF/spring.factories` 自动注册，业务方使用原生启动方式即可生效。除 `wymix.monitor.enabled` 开关外，本模块复用 Spring 标准监控配置，无其他专有配置前缀。

## 依赖引入

```xml
<dependency>
    <groupId>org.wymix.framework</groupId>
    <artifactId>monitor</artifactId>
    <version>${revision}</version>
</dependency>
```

> 前置条件：本模块不引入 `spring-boot-starter-web`，Actuator 的 HTTP 端点（如 `/actuator/prometheus`）需业务方应用自带 Web 依赖方可访问。

## 配置项

本模块使用 Spring Boot Actuator / Micrometer 标准配置，另有专有开关 `wymix.monitor.enabled`（默认 `true`，设为 `false` 时 `MonitorConfiguration` 不装配、监听器不注入默认值）。开关开启时，以下默认值会由 `MonitorApplicationStartedEventListener` 注入；注入源位于属性源列表最后（`addLast`，优先级最低），业务配置可覆盖：

| 默认注入项 | 值 |
|-----------|-----|
| `management.endpoints.web.exposure.include` | `*` |
| `management.endpoint.health.showDetails` | `always` |
| `management.endpoint.prometheus.enabled` | `true` |
| `management.metrics.export.prometheus.enabled` | `true` |
| `management.metrics.tags.application` | `${spring.application.name:app}` |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `MonitorConfiguration` | 自动装配：注册 `monitorIgnoreUrlContext` Bean（`IgnoreUrlContext`），自动放行 `/actuator/**`（配合 security 模块免认证）；受 `wymix.monitor.enabled`（默认 `true`）控制 |

## 继承类 / 扩展点

| 扩展点 | 说明 |
|--------|------|
| `IgnoreUrlContext` Bean | 追加更多免认证路径（`MonitorConfiguration` 装配且开关开启时生效） |

## 使用示例

```java
// 启动类：原生写法即可，无需任何框架启动入口类。
// MonitorApplicationStartedEventListener 已通过 META-INF/spring.factories 自动注册，
// 在环境准备阶段（ApplicationEnvironmentPreparedEvent）注入下列 Actuator 默认值
// （开关 wymix.monitor.enabled，默认 true；设为 false 可整体关闭注入与装配）：
//   wymix:
//     monitor:
//       enabled: false
public static void main(String[] args) {
    SpringApplication.run(App.class, args);
}
```

```bash
# 查看指标（需业务方应用已引入 spring-boot-starter-web）
curl http://localhost:8080/actuator/prometheus
```
