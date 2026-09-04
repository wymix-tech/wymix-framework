# travelsky_dev_web Web 应用模块

> Web 层通用能力：统一响应、TraceId 透传、Long→String 防精度丢失、i18n、慢请求监控与 CRUD 基类。

## 模块说明

`travelsky_dev_web` 为 RESTful 接口提供横切能力：`ControllerResponseAdvice` 统一包装响应、`TraceId` 在链路中透传、`Long` 序列化/反序列化为字符串避免 JS 精度丢失、i18n 国际化、`SlowRequestAspect` 慢请求监控、以及 `AbstractBaseController` 这套开箱即用的 CRUD 控制器基类。同时提供 Swagger（`swagger.web.enabled` 开关）与 CORS 等 Web 配置。

## 功能说明

| 能力 | 关键类 | 说明 |
|------|--------|------|
| 统一响应 | `ControllerResponseAdvice` | 统一包装返回结构 |
| TraceId | `RequestContextHolder` | 请求链路 ID 透传 |
| 精度防护 | Long↔String 序列化 | 前后端 Long 字段以字符串传输 |
| 国际化 | `MessageResourceHelper` | 消息资源管理 |
| 慢请求监控 | `SlowRequestAspect` | 超阈值请求告警/记录 |
| 防重复提交 | `RepeatRequest` / `RepeatRequestAspect` | 注解式防重复提交（部分实现被注释，需注意） |
| CRUD 基类 | `AbstractBaseController<T,V,ID,Service>` | 泛型化标准 CRUD 端点（getOne/findAll/save/update/delete + 批量） |
| API 文档 | `AbstractApiDocConfiguration` | Swagger，需 `swagger.web.enabled=true` 启用 |

## 配置项

| 配置前缀 | 说明 | 默认 |
|----------|------|------|
| `swagger.web.enabled` | 是否启用 Swagger 文档 | false |
| `spring.mvc.cors.*` / 框架 CORS 配置 | 跨域配置 | — |

> 其余能力由自动配置默认启用，无需单独开关。

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
//            DELETE /sampleUser/{id}、POST /sampleUser/findAll（分页）等

// 2) 启用 Swagger
// application.yml
// swagger.web.enabled: true
```

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_web</artifactId>
    <version>${revision}</version>
</dependency>
```
