# travelsky_dev_threadpool 线程池模块

> 统一线程池注册中心：支持运行时监控与参数调整，内置 TTL 上下文透传。

## 模块说明

`travelsky_dev_threadpool` 封装 `ThreadPoolExecutor` 与 `ScheduledThreadPoolExecutor`，提供命名线程池的集中注册（`ThreadPoolRegistry`）、拒绝策略配置，以及通过 Web 接口在运行时查看/调整线程池状态的能力。所有提交到托管线程池的任务都会自动透传 `TransmittableThreadLocal`（TTL）上下文，子线程可继承父线程的 traceId、用户身份等。

## 功能说明

- **线程池注册中心** `ThreadPoolRegistry`：按名称注册/获取普通线程池与定时线程池。
- **运行时可观测**：`ThreadPoolWatchController` 暴露监控端点，`ThreadPoolOperateController` 支持运行时操作（如调整参数）。
- **上下文透传**：`RunnableWrapper` / `RunnableDecorator` 自动包装任务，透传 TTL 上下文。
- **拒绝策略可配置**：内置 `Abort` / `Discard` / `DiscardOldest` / `CallerRuns`，并支持自定义实现类。

## 配置项

配置前缀：`thread-pool`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `thread-pool.pool.<name>.corePoolSize` | 普通线程池核心线程数 | 4 |
| `thread-pool.pool.<name>.maxPoolSize` | 最大线程数 | 2×corePoolSize |
| `thread-pool.pool.<name>.queueCapacity` | 队列容量 | 100 |
| `thread-pool.pool.<name>.keepAliveSeconds` | 空闲线程存活秒数 | 60 |
| `thread-pool.pool.<name>.rejectedPolicy` | 拒绝策略：`ABORT_POLICY` / `DISCARD_POLICY` / `DISCARD_OLDEST_POLICY` / `CALLER_RUNS_POLICY` / `CUSTOM_POLICY` | `ABORT_POLICY` |
| `thread-pool.pool.<name>.rejectedPolicyClassName` | 自定义拒绝策略全类名（`rejectedPolicy=CUSTOM_POLICY` 时生效） | — |
| `thread-pool.scheduled.<name>.corePoolSize` | 定时线程池核心线程数 | 4 |
| `thread-pool.scheduled.<name>.rejectedPolicy` | 定时线程池拒绝策略 | `ABORT_POLICY` |

## 使用示例

```yaml
# application.yml
thread-pool:
  pool:
    order-process:
      corePoolSize: 8
      maxPoolSize: 16
      queueCapacity: 200
      keepAliveSeconds: 60
      rejectedPolicy: CALLER_RUNS_POLICY
  scheduled:
    report-job:
      corePoolSize: 2
```

```java
// 注入使用（使用注册时配置的 name 作为 bean 名称）
@Autowired
@Qualifier("order-process")
private ExecutorWrapper orderProcessExecutor;

public void asyncWork() {
    orderProcessExecutor.submit(() -> {
        // 业务代码，自动透传 TTL 上下文（traceId / 用户身份）
    });
}
```

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_threadpool</artifactId>
    <version>${revision}</version>
</dependency>
```
