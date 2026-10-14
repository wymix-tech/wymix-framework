# web Web 应用模块

> Web 层通用能力：统一响应、TraceId 透传、Long→String 防精度丢失、i18n、慢请求监控与 CRUD 基类。

## 模块说明

`web` 为 RESTful 接口提供横切能力：`ControllerResponseAdvice` 统一包装响应、TraceId 在请求与异步线程中透传、`Long` 等数值超过 `1e14`（10 万亿）时序列化为字符串避免 JS 精度丢失，未超阈值仍按数字输出、i18n 国际化、`SlowRequestAspect` 慢请求监控、以及 `AbstractBaseController` 这套开箱即用的 CRUD 控制器基类。同时提供 Swagger 与时区等 Web 配置。本模块依赖 `springdoc-openapi-starter-webmvc-ui`（2.8.15，版本由父 pom 管理），并在 pom 中排除了 `spring-boot-starter-tomcat`、引入 `spring-boot-starter-undertow`——引入本模块后 Web 容器为 Undertow 而非 Tomcat，业务方无需再自行排除 Tomcat。

## 依赖引入

```xml
<dependency>
    <groupId>org.wymix.framework</groupId>
    <artifactId>web</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `swagger.enabled` | Swagger 总开关（关闭则不装配 API 文档） | `true` |
| `swagger.web.enabled` | 是否将 Swagger 相关路径（`/doc.html`、`/api-docs.json` 等）注册为 `IgnoreUrlContext` 忽略 URL（是否实际放行由安全模块消费逻辑决定）；仅当 `swagger.enabled=true` 且本项为 `true` 时注册 | `false` |
| `app.slow.request.inSecond` | 慢请求阈值（秒），超过即输出 WARN 日志 | 5 |
| `spring.validate.messages` | 校验消息资源 basename（数组） | `i18n/validation/ValidationMessages` |
| `user.timezone` | 应用时区（启动时设置） | `GMT+08` |
| `swagger.title` | API 文档标题（默认 `OpenAPI` 与 Auth 分组的 `securityOpenAPI` 共用） | `Api Docs` |
| `swagger.description` | API 文档描述 | `Api Docs` |
| `swagger.version` | API 文档版本号 | `1.0.0` |
| `springdoc.packages-to-scan` | 默认分组的扫描包（逗号分隔，数组绑定）；未配置则不限定包 | 空 |
| `wymix.web.repeat-request.enabled` | 是否注册框架内置的 `RepeatRequestAspect`（`@RepeatRequest` 防重复提交切面）。该开关**只控制内置实现**：业务方自行声明 `RepeatRequestAspect` Bean 时无需开启，声明即生效 | `false` |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `WebConfiguration` | 主装配：`ControllerResponseAdvice`、Long→String 序列化、`Validator`（i18n）、`SlowRequestAspect`、TraceId 过滤器、`TaskDecorator`（异步上下文透传）、时区 |
| `ApiDocAutoConfiguration` | Swagger 装配（受 `swagger.enabled` 控制），含外层装配与内部类 `AuthGroupOpenApiConfiguration`（classpath 存在 `CustomSecurityProperties` 时激活，引入 `security` 后默认生效）：<br>① 默认 `OpenAPI`（`openAPI`，`@ConditionalOnMissingBean`）——业务方声明自己的 `OpenAPI` Bean 即可覆盖；但内部类的 `securityOpenAPI`（含 bearerAuth 安全方案）**无** `@ConditionalOnMissingBean`，声明的 `OpenAPI` 不会覆盖它；<br>② 默认分组 `commonGroupOpenApi`（group=`default`，`@ConditionalOnMissingBean(GroupedOpenApi.class)`）——声明任意 `GroupedOpenApi` Bean 即不注册该分组；<br>③ Auth 分组 `authGroupOpenApi`（group=`Auth`，`@ConditionalOnMissingBean(name = "authGroupOpenApi")`）——仅当声明的 Bean 名为 `authGroupOpenApi` 时才让位，否则始终注册并附带登录端点文档；<br>④ `swagger.web.enabled=true` 时注册 `IgnoreUrlContext` 忽略 URL Bean |

## 继承类（业务方核心用法）

| 类/接口 | 说明 |
|---------|------|
| `AbstractBaseController<T, V, ID, SERVICE>` | **控制器基类**：继承即获得标准 CRUD 端点，自动完成 VO↔PO 转换（基于反射按泛型实例化 + `BeanUtils.copyProperties`） |
| `BaseController<T, V, ID>` | 控制器契约接口（含 Swagger `@Operation` 注解的标准端点定义） |

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
| `@RepeatRequest(expiredTimeInMills = 1000)` | 防重复提交（仅支持 Controller 方法）。**默认不生效**，启用方式二选一：配置 `wymix.web.repeat-request.enabled=true` 使用框架内置切面；或自行声明 `RepeatRequestAspect` Bean（此时无需配置开关）。⚠️ 去重键存放在 `HttpSession`，仅同一会话内有效，无状态 / 分布式部署需自行改造 |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `mappedToVO` / `mappedToPO` | 控制器基类的 VO↔PO 转换方法，可覆写替换默认反射实现 |
| `TaskDecorator` Bean | `@ConditionalOnMissingBean`，可自定义异步线程上下文装饰（默认透传请求上下文 + customTraceId；已有默认实现时自动让位） |
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

// 2) 启用 Swagger：引入 web 即由 ApiDocAutoConfiguration 自动装配，无需继承任何基类
//    通过 swagger.enabled=false 关闭，springdoc.packages-to-scan 指定扫描包
```
