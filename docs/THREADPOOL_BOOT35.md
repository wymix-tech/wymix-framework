# Spring Boot 3.5 线程池修复与迁移

## 默认池所有权

默认 `applicationTaskExecutor` 和 `taskScheduler` 由 Boot 原生配置管理。core 只启用注解并提供 Customizer，不再通过同名 Bean 争夺默认池。因此普通装配与提前导入的 `force` 模式都不会产生框架/Boot 同名冲突。

用户 `AsyncConfigurer`、自定义 `applicationTaskExecutor`、其他名称的 `TaskScheduler` 按 Boot/Spring 原生规则生效。只提供普通 `taskExecutor` 时，Boot 正常模式会退让，此时 MVC/WebFlux 所需执行器需由业务另行提供；也可配置 `spring.task.execution.mode=force` 保留 Boot 默认池。`force` 下常规 @Async 由 Boot 默认池执行，除非业务提供 AsyncConfigurer。显式 `@Async("business-pool")` 指向业务命名池。

兼容类 `TaskThreadPoolExecutor` / `ScheduledTaskThreadPoolExecutor` 标记为弃用，直接继承 Spring 当前原生实现，保留原生访问、ThreadFactory、装饰器、暂停与关闭行为。原先复制的旧实现、无写入的 decoratedTaskMap 和进程级 AOP system property 已移除。

## 推荐配置

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 4
        max-size: 8
        queue-capacity: 300
        keep-alive: 60s
      thread-name-prefix: application-
      shutdown:
        await-termination: true
        await-termination-period: 60s
    scheduling:
      pool:
        size: 4
      thread-name-prefix: application-schedule-
```

此示例显式保留以前文档建议的容量与前缀。未配置时使用 Boot 默认值，不再应用旧 AsyncScheduleProperties 的隐式默认值。显式 `async.*` 参数仍兼容，标准 `spring.task.*` 同项优先；映射表见 core/README.md。`async.enabled=false` 只关闭框架注解启用与兼容 Customizer，Boot 自身行为仍存在。

默认 Spring 池通过 TaskDecorator 扩展上下文传播。TTL 自动透传保留在命名业务池；如默认 Spring 池也需要 TTL，请声明 `TaskDecorator ttlDecorator() { return com.alibaba.ttl.TtlRunnable::get; }`，框架会与其他装饰器按顺序组合。

Java 21+ 可设置 `spring.threads.virtual.enabled=true`，由 Boot 选择 SimpleAsync 实现；平台池尺寸参数不控制虚拟线程。Java 17 不启用虚拟线程。多个 TaskDecorator 按顺序组合。上下文装饰器恢复执行线程原有请求上下文和 MDC，避免 CallerRunsPolicy / 嵌套执行清空外层值。

## 业务命名池

`thread-pool.pool.*` / `thread-pool.scheduled.*` 通过 BeanDefinition 注册，类型、候选标记与销毁由容器管理。业务池 `defaultCandidate=false`，不会让 Boot 默认池消失。通过 @Qualifier 或显式 @Async 名称使用。

业务池名不得占用 `applicationTaskExecutor`、`taskExecutor`、`taskScheduler`、`bootstrapExecutor`，与已有 Bean 或别名冲突也会启动失败。若曾用这些配置名称模拟默认池，请改用标准 Spring 配置或显式业务 Bean。

普通池 max 未配置时取 2×core；显式 max<core、负队列或非法拒绝类型启动失败，不再静默修正配置。队列容量 0 使用 SynchronousQueue。容器销毁立即关闭业务池、取消排队任务并注销；静态注册 API 的池仍由调用方管理。

## 监控契约

原生 Spring 平台池按 Bean 名登记，不替换执行器；监控适配器的 execute/submit 仍经 Spring owner，保留装饰器与异常契约。虚拟线程执行器没有传统队列/固定池尺寸，不注册虚假的平台池指标。

poolSize 为真实线程数，corePoolSize 为配置核心数。监控读取实际底层线程、活动数、队列与累计任务数；动态调参同步 Spring owner 的尺寸设置，扩容先最大数、缩容先核心数。

**检查 outcomeMetricsAvailable 后再使用成功率、失败率与耗时。** 原生线程池无法可靠读出所有 CompletableFuture 等回调的业务结果，因此返回 false，这些结果字段不应展示为真实数值。命名业务池可观察提交返回 true；提交不透明 CompletableFuture 内部回调后返回 false，避免虚假成功率。

```java
@Qualifier("order-process")
ExecutorWrapper pool;

CompletableFuture<Result> result = pool.submitCompletable(() -> doWork());
```

这个现代提交入口在普通池和调度池保留业务异常及取消传播。旧 Spring ListenableFuture 接口仅通过兼容类继承保留，不再由框架复制实现。

## 拒绝与取消

- Abort：抛出拒绝异常，不留下未完成计数。
- Discard：取消被丢弃的 Future。
- CallerRuns：只执行一次，保留正常 Future；Web 装饰器恢复调用者原上下文。
- DiscardOldest：取消旧排队任务的 Future，新任务重新入队只包装一次；无可淘汰队列项时明确拒绝，避免递归。
- 自定义策略：支持抛异常、同步执行、通过回调传入的原生池重新提交。自行移除排队任务时应终结其 Future；异步转交其他池需自定义统计契约。

## 回归覆盖

测试覆盖默认装配、旧/新配置优先级、提前导入+force、用户 TaskExecutor/AsyncConfigurer/TaskScheduler、MVC Callable、装饰器组合、暂停与协调停止、调度关闭策略、运行期调参、ThreadFactory、四种拒绝策略、取消、现代 Future 结果、命名池候选及销毁。

Java 21 原生虚拟线程选择测试通过运行时条件启用；Java 17 环境跳过该用例。CI 已配置 Java 17/21 matrix，只有实际完成后才能声称 Java 21 已验证。

## 官方依据

- https://docs.spring.io/spring-boot/3.5/reference/features/task-execution-and-scheduling.html
- https://docs.spring.io/spring-boot/3.5/api/java/org/springframework/boot/task/ThreadPoolTaskExecutorCustomizer.html
- https://docs.spring.io/spring-boot/3.5/api/java/org/springframework/boot/task/ThreadPoolTaskSchedulerCustomizer.html
- https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/scheduling/concurrent/ExecutorConfigurationSupport.html

## 本地验证结果（2026-10-14）

Java 17 / Maven 3.9.11 全工程 clean verify 成功，MyBatis/JPA 两套示例均编译打包。追加自定义拒绝策略子类覆盖测试后，线程池模块 22 项回归通过；当前所有模块报告按 testcase 统计合计 189 项，188 通过、0 失败、0 错误、1 项 Java 21 条件用例跳过。Java 21 的实际结果以 CI 为准。
