# travelsky_dev_core 核心模块

> 框架启动入口与横切能力中枢：启动加载、异步调度、Feign 动态服务发现、邮件与消息发送。

## 模块说明

`travelsky_dev_core` 是框架的核心模块，承载不依附于具体持久化/安全方案的通用能力：`BootLoader` 统一启动入口、`AbstractAsyncScheduleConfiguration` 异步与定时线程池、`CustomFeignConfiguration` + 动态实例管理的 Feign 负载均衡、以及基于 `spring.mail` 的邮件发送。它是上层应用与框架能力之间的桥梁。

## 功能说明

- **启动加载** `BootLoader`：统一的应用启动引导。
- **异步 / 定时调度**：`AbstractAsyncScheduleConfiguration` 根据 `async.*` 注入 `AsyncTaskExecutor` 与 `ScheduledExecutorService`；`TaskThreadPoolExecutor` / `ScheduledTaskThreadPoolExecutor` 提供可监控的线程池实现。
- **Feign 动态服务发现**：`DynamicServiceInstanceSupplier` / `DynamicInstanceManager` 配合 `FeignApplicationStartedEventListener`，从配置 `http.requests.*` 读取实例清单，运行时动态刷新服务实例，无需注册中心。
- **负载均衡**：`GlobalLoadBalancerConfiguration` 提供全局负载均衡策略。
- **邮件 / 消息**：`MessageSenderService` / `EmailSenderService` 在配置了 `spring.mail.host` 时自动启用。
- **上下文工具**：`ApplicationContextHolder` 提供静态获取 Spring Bean 的能力。

## 配置项

| 配置前缀 | 说明 | 默认 |
|----------|------|------|
| `async.corePoolSize` | 异步线程池核心线程数 | 4 |
| `async.maxPoolSize` | 异步线程池最大线程数 | 8 |
| `async.queueCapacity` | 队列容量 | 300 |
| `async.threadNamePrefix` | 线程名前缀 | `application-` |
| `async.keepAliveSeconds` | 空闲存活秒数 | 60 |
| `async.waitForTasksToCompleteOnShutdown` | 关闭时等待任务完成 | true |
| `async.awaitTerminationSeconds` | 关闭等待超时秒 | 60 |
| `async.schedule.corePoolSize` | 定时线程池核心线程数 | 4 |
| `async.schedule.threadNamePrefix` | 定时线程名前缀 | `application-schedule-` |
| `spring.mail.*` | 标准 Spring Mail 配置（配置 host 即启用邮件能力） | — |
| `http.requests.<svc>` | Feign 动态实例地址清单（供 `DynamicServiceInstanceSupplier` 使用） | — |

## 使用示例

```java
// 1) 启用异步调度：在启动类或配置类导入
@Import(AbstractAsyncScheduleConfiguration.class)
@EnableAsync
public class AppConfig {}

// 2) 异步执行
@Async
public CompletableFuture<Result> doAsync() { ... }

// 3) 发送邮件（需配置 spring.mail.host）
@Autowired
private EmailSenderService emailSenderService;

// 4) Feign 动态实例（无需注册中心）
// application.yml
// http.requests.user-service[0]=http://10.0.0.1:8080
// http.requests.user-service[1]=http://10.0.0.2:8080
```

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_core</artifactId>
    <version>${revision}</version>
</dependency>
```
