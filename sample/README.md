# sample 示例应用

> 框架能力的端到端可运行示例，演示各模块的集成方式。

## 模块说明

`sample` 是一个完整的 Spring Boot 应用，把框架各模块串联起来作为「用法示范」。它包含：动态数据源配置、安全配置（内存用户）、Feign + 异步配置、以及一套标准的 Entity/VO/Service/Controller CRUD 示例（`SampleUser*`）。可作为新项目脚手架的参考模板。

## 功能说明

| 示例 | 文件 | 演示点 |
|------|------|--------|
| 实体 | `SampleUserEntity`（继承 `AbstractAuditEntity`） | 审计字段自动填充 |
| VO | `SampleUserVO` | PO/V0 分离 |
| 服务 | `SampleUserService` | 业务服务 |
| 控制器 | `SampleUserController`（继承 `AbstractBaseController`） | 开箱 CRUD 端点 |
| 安全 | `SecurityConfiguration extends CustomSecurityConfiguration` | 注入 `UserDetailsService` |
| Feign | `FeignConfiguration` / `Test1Api` / `Test2Api` | 声明式 RPC |
| 异步 | `AsyncConfiguration` | 异步线程池 |
| 启动 | `ServerRunner` | 启动钩子 |

## 配置文件

| 文件 | 说明 |
|------|------|
| `application.yml` | 主配置（端口、应用名等） |
| `application-datasource.yml` | 数据源配置示例 |
| `application-security.yml` | 安全配置示例（JWT / 白名单） |

## 使用示例

```java
// SampleUserController —— 继承即拥有标准 CRUD 端点
@RestController
@RequestMapping("sampleUser")
public class SampleUserController
        extends AbstractBaseController<SampleUserEntity, SampleUserVO, Long, SampleUserService>
        implements BaseController<SampleUserEntity, SampleUserVO, Long> {

    public SampleUserController(SampleUserService service) {
        super(service);
    }
}
```

```bash
# 运行示例
mvn -pl sample -am spring-boot:run

# 调用 CRUD
curl -X POST http://localhost:8080/sampleUser \
     -H 'Content-Type: application/json' \
     -d '{"username":"alice","password":"123"}'
```

## 依赖引入

`sample` 自身不发布为依赖，仅作为可运行示例；查看其 `pom.xml` 了解应引入哪些框架模块。
