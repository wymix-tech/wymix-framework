# travelsky_dev_web Web 应用模块

> Web 层通用能力：统一响应、TraceId 透传、Long→String 防精度丢失、i18n、慢请求监控与 CRUD 基类。

## 模块说明

`travelsky_dev_web` 为 RESTful 接口提供横切能力：`ControllerResponseAdvice` 统一包装响应、TraceId 在请求与异步线程中透传、`Long` 序列化为字符串避免 JS 精度丢失、i18n 国际化、`SlowRequestAspect` 慢请求监控、以及 `AbstractBaseController` 这套开箱即用的 CRUD 控制器基类。同时提供 Swagger 与时区等 Web 配置。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_web</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `swagger.enabled` | Swagger 总开关（关闭则不装配 API 文档） | `true` |
| `swagger.web.enabled` | 是否注册 Swagger 基础信息与分组 | `false` |
| `app.slow.request.inSecond` | 慢请求阈值（秒），超过即输出 WARN 日志 | 5 |
| `spring.validate.messages` | 校验消息资源 basename（数组） | `i18n/validation/ValidationMessages` |
| `user.timezone` | 应用时区（启动时设置） | `GMT+08` |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `WebConfiguration` | 主装配：`ControllerResponseAdvice`、Long→String 序列化、`Validator`（i18n）、`SlowRequestAspect`、TraceId 过滤器、`TaskDecorator`（异步上下文透传）、时区 |
| `ApiDocAutoConfiguration` | Swagger 装配（受 `swagger.enabled` 控制） |

## 继承类（业务方核心用法）

| 类/接口 | 说明 |
|---------|------|
| `AbstractBaseController<T, V, ID, SERVICE>` | **控制器基类**：继承即获得标准 CRUD 端点，自动完成 VO↔PO 转换（基于反射按泛型实例化 + `BeanUtils.copyProperties`） |
| `BaseController<T, V, ID>` | 控制器契约接口（含 Swagger `@Operation` 注解的标准端点定义） |
| `AbstractApiDocConfiguration` | Swagger 文档配置基类，需实现 `packageToScan()` 指定扫描包 |

### AbstractBaseController 自动暴露的端点

| 方法 | 端点 |
|------|------|
| `getOne(id)` | `GET /{id}` |
| `findAll(DevQueryVO)` | `GET /`（分页 + filter DSL） |
| `save(v)` | `POST /` |
| `update(id, v)` | `PUT /{id}` |
| `delete(id)` | `DELETE /{id}` |
| `saveBatch(vs)` | `POST /batch` |
| `updateBatch(vs)` | `PUT /batch` |
| `deleteBatch(ids)` | `DELETE /batch` |

## 注解（业务方使用）

| 注解 | 说明 |
|------|------|
| `@RepeatRequest(expiredTimeInMills = 1000)` | 防重复提交（仅支持 Controller 方法；当前切面 Bean 注册被注释，需自行声明 `RepeatRequestAspect` Bean 启用） |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `mappedToVO` / `mappedToPO` | 控制器基类的 VO↔PO 转换方法，可覆写替换默认反射实现 |
| `TaskDecorator` Bean | `@ConditionalOnMissingBean`，可自定义异步线程上下文装饰（默认透传请求上下文 + customTraceId） |
| `RequestContextHolder` | 静态获取当前 `HttpServletRequest` |
| `MessageResourceHelper` | i18n 消息工具 |

## 使用示例

```java
// 1) 继承 AbstractBaseController 即获得标准 CRUD 端点
@RestController
@RequestMapping("sampleUser")
public class SampleUserController
        extends AbstractBaseController<SampleUserEntity, SampleUserVO, Long, SampleUserService>
        implements BaseController<SampleUserEntity, SampleUserVO, Long> {

    public SampleUserController(SampleUserService service) {
        super(service);
    }
}
// 自动暴露：GET /sampleUser/{id}、POST /sampleUser、PUT /sampleUser/{id}、
//            DELETE /sampleUser/{id}、GET /sampleUser（分页）等

// 2) 启用 Swagger：继承 AbstractApiDocConfiguration 并配置 swagger.web.enabled=true
```
