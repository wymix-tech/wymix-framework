# travelsky_dev_monitor 监控模块

> 基于 Actuator + Micrometer 的应用可观测性：指标暴露与启动事件采集。

## 模块说明

`travelsky_dev_monitor` 在 Spring Boot Actuator 与 Micrometer 之上，暴露应用运行指标（JVM、HTTP、线程池等），并通过 `MonitorApplicationStartedEventListener` 在环境准备阶段注入 Actuator/Prometheus 默认配置，`MonitorBootLoader` 提供监控版启动入口。本模块复用 Spring 标准监控配置，无专有配置前缀。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_monitor</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

本模块使用 Spring Boot Actuator / Micrometer 标准配置。启用 `MonitorBootLoader` 后以下默认值会被注入（业务配置可覆盖，因注入源在最后）：

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
| `MonitorConfiguration` | 自动装配：注册 `IgnoreUrlContext` Bean，自动放行 `/actuator/**`（配合 security 模块免认证） |

## 继承类 / 扩展点

| 扩展点 | 说明 |
|--------|------|
| `MonitorBootLoader.genApp(Class)` | **启动入口**：`SpringApplication app = MonitorBootLoader.genApp(App.class)`，自动挂载监控事件监听器（内部复用 `BootLoader`） |
| `BootLoader.addListener(...)` | 追加自定义启动监听器 |
| `IgnoreUrlContext` Bean | 追加更多免认证路径 |

## 使用示例

```java
// 启动类（使用监控版 BootLoader，自动注入 Actuator 默认配置）
public static void main(String[] args) {
    MonitorBootLoader.genApp(App.class).run(args);
}
```

```bash
# 查看指标
curl http://localhost:8080/actuator/prometheus
```
