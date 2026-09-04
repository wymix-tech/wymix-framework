# travelsky_dev_monitor 监控模块

> 基于 Actuator + Micrometer 的应用可观测性：指标暴露与启动事件采集。

## 模块说明

`travelsky_dev_monitor` 在 Spring Boot Actuator 与 Micrometer 之上，暴露应用运行指标（JVM、HTTP、线程池等），并通过 `MonitorApplicationStartedEventListener` 在应用启动完成后采集/上报监控信息，`MonitorBootLoader` 提供监控相关的启动钩子。本模块复用 Spring 标准监控配置，无专有配置前缀。

## 功能说明

- **指标暴露**：集成 Micrometer + Prometheus 注册表，暴露 `/actuator/prometheus` 等端点（需配合 Actuator 配置）。
- **启动事件采集** `MonitorApplicationStartedEventListener`：在 `ApplicationStartedEvent` 时执行监控初始化。
- **启动钩子** `MonitorBootLoader`：监控模块自身的启动加载。

## 配置项

本模块使用 Spring Boot Actuator / Micrometer 标准配置，无专有前缀：

| 配置前缀 | 说明 |
|----------|------|
| `management.endpoints.web.exposure.include` | 暴露的端点（如 `health,info,metrics,prometheus`） |
| `management.metrics.tags.*` | 指标全局标签（如 `application=${spring.application.name}`） |
| `management.prometheus.metrics.export.*` | Prometheus 导出配置 |

## 使用示例

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,threadpool
  metrics:
    tags:
      application: ${spring.application.name}
  prometheus:
    metrics:
      export:
        enabled: true
```

```bash
# 查看指标
curl http://localhost:8080/actuator/prometheus
```

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_monitor</artifactId>
    <version>${revision}</version>
</dependency>
```
