# threadpool 线程池模块

> **当前版本兼容变化**：请先阅读 [评审修复与接入说明](../docs/REVIEW_REPAIR.md)。新项目推荐 MyBatis，JPA 仅兼容维护。

> 统一线程池注册中心：支持运行时监控与参数调整，内置 TTL 上下文透传。

## 模块说明

`threadpool` 封装 `ThreadPoolExecutor` 与 `ScheduledThreadPoolExecutor`，提供命名线程池的集中注册（`ThreadPoolRegistry`）、拒绝策略配置，以及运行时查看/调整线程池状态的能力。所有提交到命名业务线程池的任务都会自动透传 `TransmittableThreadLocal`（TTL）上下文，子线程可继承父线程的 traceId、用户身份等。

## 依赖引入

```xml
<dependency>
    <groupId>org.wymix.framework</groupId>
    <artifactId>threadpool</artifactId>
    <version>${revision}</version>
</dependency>
```

`transmittable-thread-local`（TTL）为本模块编译期强依赖，随本模块自动带入，业务方无需额外引入。

## 配置项

自动装配开关：`wymix.threadpool.enabled`（默认 `true`）。线程池参数由 `ThreadPoolConfiguration` 在启动期直接从 `thread-pool` 前缀绑定读取。

配置前缀：`thread-pool`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `thread-pool.pool.<name>.corePoolSize` | 普通线程池核心线程数 | 4 |
| `thread-pool.pool.<name>.maxPoolSize` | 最大线程数，必须 ≥ corePoolSize；显式非法值启动失败 | 未配置时为 2×corePoolSize |
| `thread-pool.pool.<name>.queueCapacity` | 队列容量（正数为 `LinkedBlockingQueue`，0 为 `SynchronousQueue`） | 100 |
| `thread-pool.pool.<name>.keepAliveSeconds` | 空闲线程存活秒数 | 60 |
| `thread-pool.pool.<name>.rejectedPolicy` | 拒绝策略：`ABORT_POLICY` / `DISCARD_POLICY` / `DISCARD_OLDEST_POLICY` / `CALLER_RUNS_POLICY` / `CUSTOM_POLICY` | `ABORT_POLICY` |
| `thread-pool.pool.<name>.rejectedPolicyClassName` | 自定义拒绝策略全类名（反射实例化），仅当 `rejectedPolicy=CUSTOM_POLICY` 时生效 | — |
| `thread-pool.scheduled.<name>.corePoolSize` | 定时线程池核心线程数 | 4 |
| `thread-pool.scheduled.<name>.rejectedPolicyClassName` | 定时线程池自定义拒绝策略全类名，仅当 `rejectedPolicy=CUSTOM_POLICY` 时生效 | — |

> 未配置任何 `pool.<name>` / `scheduled.<name>` 时不会创建默认线程池。
> `scheduled.<name>` 复用同一参数结构，仅 `corePoolSize` 与拒绝策略相关项生效。

> 每个业务线程池通过 BeanDefinition 注册，由容器初始化和销毁，需使用 `@Qualifier` 或 `@Async("<name>")`。`defaultCandidate=false` 保留 Boot 默认执行器。业务池名不得使用 `applicationTaskExecutor` / `taskExecutor` / `taskScheduler` / `bootstrapExecutor`；重复名称启动失败。
>
> 模块也观察 Spring 原生平台池，按 Bean 名（如 `applicationTaskExecutor`）提供监控和动态调参，保持原 Bean 身份与生命周期。无实际线程池的虚拟线程执行器不伪装为平台池。

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `ThreadPoolConfiguration` | 自动装配入口，读取 `thread-pool.*` 配置并注册线程池 Bean |
| `NativeThreadPoolMonitor` / `NativeThreadPoolAdapter` | 观察原生 Spring 平台池，实际线程数、活动数、队列与运行期配置；不替换原 Bean |
| `ThreadPoolRegistry` | 静态注册中心：`registerThreadPool` / `registerScheduledThreadPool` / `newFixedThreadPool` / `newSingleThreadExecutor` / `newScheduledThreadPool` |
| `ThreadPoolMonitor` | 运行时线程池状态查询 |
| `ThreadPoolOperator` | 运行时参数调整（corePoolSize / maxPoolSize / keepAliveTime） |

## 继承类（业务方可继承/实现）

| 类/接口 | 说明 |
|---------|------|
| `ExecutorWrapper` | 托管线程池统一接口（继承 `ExecutorService`），注入后 `submit`/`execute` |
| `RunnableDecorator` / `RunnableWrapper` | 任务包装扩展点，实现后可自定义任务装饰（默认透传 TTL 上下文） |
| `RejectedExecutionHandler` | 自定义拒绝策略：实现后配置 `rejectedPolicyClassName` 指向全类名 |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `ThreadPoolRegistry` 静态 API | 代码中随时注册额外线程池 |
| `ThreadPoolWatchController` | 查看全部线程池状态（`ThreadPoolMonitor.allThreadPoolContext()`）；未自动注册为 Bean，需业务方自行注册并暴露 |
| `ThreadPoolOperateController` | 运行时调整线程池参数（`changeThreadPoolConfig`），maxPoolSize ≥ corePoolSize 校验；未自动注册为 Bean，需业务方自行注册并暴露 |

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

## 结果统计与现代异步 API

`ExecutorWrapper.submitCompletable(Runnable/Callable)` 返回 `CompletableFuture`，保留业务异常与取消传播。普通/调度业务池可识别该入口的结果。推荐使用它提交需要结果统计的任务，而不是 `CompletableFuture.supplyAsync(..., pool)` 的不透明内部回调。

`ThreadPoolContext.poolSize` 为真实线程数量，`corePoolSize` 为配置值。消费成功率、失败率、耗时前先检查 `outcomeMetricsAvailable`：

- 业务池的可观察提交返回 true。
- 原生 Spring 池不能可靠提供完整业务结果与耗时，返回 false；对应数值为兼容占位，不能解释为零失败或成功。
- 业务池执行不透明 `CompletableFuture` 回调后也返回 false，避免错误宣称 100% 成功。

`DiscardOldestPolicy` 将被移除的旧 Future 取消，并让新任务只入队/执行一次；容量 0 无法淘汰旧任务时明确拒绝，避免递归。自定义拒绝策略需抛异常、同步执行或通过传入的原生池重新提交；若自行移除任务，必须明确终结该任务的 Future。异步转交其他执行器不在内置统计契约内。

容器销毁时业务池立即关闭、取消待执行任务并注销。静态 API 注册的池仍由调用方负责关闭，可调用包装器 `destroy()` 一并注销。

详见 [线程池 3.5 迁移说明](../docs/THREADPOOL_BOOT35.md)。
