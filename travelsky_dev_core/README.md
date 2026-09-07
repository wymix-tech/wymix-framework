# travelsky_dev_core 核心模块

> 框架启动入口与横切能力中枢：启动加载、异步调度、Feign 动态服务发现、邮件与消息发送。

## 模块说明

`travelsky_dev_core` 是框架的核心模块，承载不依附于具体持久化/安全方案的通用能力。引入依赖即自动生效（可用 `travelsky.core.enabled=false` 关闭基础装配、`async.enabled=false` 关闭异步装配）。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_core</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

### 异步 / 调度线程池（前缀 `async`）

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `async.enabled` | 是否启用异步/调度自动装配 | `true` |
| `async.corePoolSize` | 异步线程池核心线程数 | 4 |
| `async.maxPoolSize` | 异步线程池最大线程数 | 8 |
| `async.queueCapacity` | 队列容量 | 300 |
| `async.threadNamePrefix` | 线程名前缀 | `application-` |
| `async.keepAliveSeconds` | 空闲存活秒数 | 60 |
| `async.waitForTasksToCompleteOnShutdown` | 关闭时等待任务完成 | `true` |
| `async.awaitTerminationSeconds` | 关闭等待超时秒 | 60 |
| `async.schedule.poolSize` | 定时线程池核心线程数 | 4 |
| `async.schedule.threadNamePrefix` | 定时线程名前缀 | `application-schedule-` |

### 基础装配与其他

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `travelsky.core.enabled` | 是否启用核心自动装配 | `true` |
| `spring.mail.host` | 配置后自动注册 `EmailSenderService`（邮件能力开关） | — |
| `http.requests.<svc>` | Feign 动态实例清单，启动期自动映射为 `spring.cloud.loadbalancer.*` 配置 | — |
| `http.requests.<svc>.retry.*` | 重试配置（`enabled` / `retryOnAllOperations` / `maxRetriesOnSameServiceInstance` 等） | — |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `CoreConfiguration` | 基础装配：`ApplicationContextHolder`、`MessageService`、条件装配 `EmailSenderService` |
| `AsyncScheduleAutoConfiguration` | 异步/调度统一装配：注册 `applicationTaskExecutor` / `taskExecutor` / `taskScheduler` |

## 继承类（业务方可继承/实现）

| 类/接口 | 说明 |
|---------|------|
| `AbstractAsyncScheduleConfiguration` | 旧式异步配置基类（`@Deprecated(since = "3.x", forRemoval = true)`），需实现抽象方法 `taskDecorator()`，仍兼容 |
| `MessageSenderService` | 消息发送 SPI 接口，实现后注册为 Bean 即可接入统一消息发送 |
| `CustomFeignConfiguration` | Feign 基础配置抽象类（`decentralization_discovery` 模块继承使用） |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `BootLoader.genApp(Class)` | **启动入口**：`SpringApplication app = BootLoader.genApp(App.class)`，自动挂载 Feign 动态实例与日志监听器；`BootLoader.addListener(...)` 注册自定义监听器 |
| `TaskDecorator` Bean | 声明即覆盖默认装饰器（如 `web` 模块用它透传请求上下文与 TraceId） |
| 名为 `taskExecutor` 的 `Executor` Bean | 完全接管异步线程池 |
| `AuthenticationEntryPoint` 等标准 Bean | 通过 `@ConditionalOnMissingBean` 支持覆盖默认实现 |
| `ApplicationContextHolder` | 静态获取 Spring 容器 Bean |
| `MessageSenderService` 实现类 | 收集容器内所有实现，统一由 `Message` 消息模型分发（邮件、短信等） |

## 使用示例

```java
// 1) 启动类（框架标准写法）
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        BootLoader.genApp(App.class).run(args);
    }
}

// 2) 异步执行（引入依赖即生效，async.enabled=false 可关闭）
@Async
public CompletableFuture<Result> doAsync() { ... }

// 3) 发送邮件（需配置 spring.mail.host 与 spring.mail.username）
@Autowired
private EmailSenderService emailSenderService;

// 4) Feign 动态实例（无需注册中心）
// application.yml
// http.requests.user-service[0]=http://10.0.0.1:8080
// http.requests.user-service[1]=http://10.0.0.2:8080
```

## 注意事项

- `AsyncScheduleAutoConfiguration` 刻意**不**实现 `AsyncConfigurer`，避免与 Boot 默认装配冲突导致 “Only one AsyncConfigurer may exist”。
- Feign 动态实例配置在 `ApplicationEnvironmentPreparedEvent` 阶段被改写为 `spring.cloud.loadbalancer.*`，因此无需注册中心。
