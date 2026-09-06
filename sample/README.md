# sample 示例应用

> 框架能力的端到端可运行示例：引入框架全部模块，并给每个模块配一套可调用的示例端点。

## 模块说明

`sample` 是一个完整的 Spring Boot 应用，把框架各模块串联起来作为「用法示范」。除原有的 Entity/VO/Service/Controller CRUD 示例（`SampleUser*`）外，
`com.swcares.sample.demo` 下按模块划分了示例包，每个模块都有对应的 Controller 与配置。

## 模块 → 示例对照

| 模块 | 示例入口 | 演示点 |
|------|----------|--------|
| `travelsky_dev_core` | `demo/core/*` · `DemoMessageSender` · `DemoAsyncTask` | `ApplicationContextHolder` 静态取 Bean、`MessageService` 消息门面 + `MessageSenderService` SPI、`@Async` 异步 |
| `travelsky_dev_common` | `demo/common/*` | `ResultVO` 统一响应、`DevQueryVO` 过滤 DSL、`Page` 分页、`UserContextHolder` 登录上下文、`BusinessException` |
| `travelsky_dev_utils` | `demo/utils/*` | `JsonUtils`、`GlobalIdGenerator`、`StringUtils`、`DateUtils`、`BeanUtils` |
| `travelsky_dev_web` | `demo/web/*` | `ControllerResponseAdvice` 自动包装响应、`@RepeatRequest` 防重提交 |
| `travelsky_dev_security` | `demo/security/*` · `SecurityConfiguration` | JWT 签发/校验（密钥取自 `security.jwt.*`）、`ENC()` 配置解密 |
| `travelsky_dev_cache` | `demo/cache/*` · `CacheDemoService` | `@Cacheable`/`@CachePut`/`@CacheEvict`（sample 选用 Caffeine） |
| `travelsky_dev_threadpool` | `demo/threadpool/*` | 按 `thread-pool.*` 自动注册命名线程池、`ExecutorWrapper` 运行期观测与调参、`ThreadPoolHolder` |
| `travelsky_dev_mybatis_repository` | `SampleUserDao` / `SampleUserService` / `SampleUserController` | `BaseRepository` CRUD、`AbstractBaseServiceImpl`、`PageHelper` 分页、`@DataPermission` + `PermissionSqlInjector` 数据权限 |
| `travelsky_dev_jpa_repository` | `pom.xml` 的 `jpa` profile | 与 mybatis profile 二选一 |
| `travelsky_dev_repository` | `SampleUserEntity` | `AbstractAuditEntity` 审计字段自动填充 |
| `travelsky_dev_data_migration` | `demo/dataMigration/*` · `SampleUserEntity` 上的 `@DataMigration` | 数据变更触发迁移 |
| `travelsky_dev_dynamic_datasource` | `demo/dynamicDatasource/*` · `DynamicDatasourceDemoService` | `@DynamicDatasource` 注解切库、`DynamicDataSourceContextHolder` 编程式切库 |
| `travelsky_dev_decentralization_discovery` | `demo/discovery/*` | 节点信息查询（需 `discovery` profile，见「已知限制」） |
| `travelsky_dev_monitor` | `ServerRunner` 中注释掉的 `MonitorBootLoader` | 放行 `/actuator/**`，启动信息打印 |
| `travelsky_dev_test` | `src/test/java/.../SampleMySqlContainer`、`SampleUserDaoTest` | Testcontainers 容器基类 + `@DataSet` 数据编排 |
| `travelsky_dev_ai` | — | 空壳占位模块，暂无自动装配内容，仅保留坐标 |

## 配置文件

| 文件 | 说明 |
|------|------|
| `application.yml` | 主配置：激活的 profile 与各模块开关汇总 |
| `application-datasource.yml` | 数据源 |
| `application-security.yml` | 安全（JWT / 白名单 / 表单登录） |
| `application-cache.yml` | Caffeine 缓存规格 |
| `application-async.yml` | 异步线程池 + `thread-pool.*` 自定义线程池（含示例用的 `testThreadPool`） |
| `application-discovery.yml` | 去中心化服务发现（opt-in） |
| `application-dynamic-datasource.yml` | 动态数据源（opt-in） |

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
# 构建并运行
mvn -pl sample -am clean package -DskipTests
java -jar sample/target/sample-2.0-SNAPSHOT.jar

# 各模块示例端点（均在 security.ignoreUrls=/demo/** 白名单内，可直接访问）
curl http://localhost:8080/demo/utils/id
curl http://localhost:8080/demo/cache/k1
curl -X POST http://localhost:8080/demo/core/message?content=hi
curl http://localhost:8080/demo/threadpool/status
curl -X POST 'http://localhost:8080/demo/security/token?username=admin'

# 切换到 JPA ORM（默认 mybatis）
mvn -pl sample -am clean package -DskipTests -P jpa -P '!mybatis'
```

## 已知限制

- **`decentralization_discovery` 暂不可直接启用**：`NodeService` 依赖 Spring Cloud OpenFeign 的 `FeignContext`，
  但父 POM 锁定的 `spring-cloud-starter-openfeign` 为 `3.1.3`（Spring Cloud 2021.x，仅提供 Boot 2 时代的 `spring.factories` 自动装配）。
  在 Boot 3.5 下 `FeignAutoConfiguration` 不会被加载，开启 `decentralization.discovery.enable=true` 会报
  `required a bean of type 'FeignContext' that could not be found`。
  需将 Spring Cloud 升级到与 Boot 3.5 匹配的 2025.x 版本后方可启用；示例 `demo/discovery/*` 已按同一开关条件装配，届时自动生效。
- **`dynamicDatasource` profile 需要真实数据库**：配置中的 MySQL 地址与 `ENC()` 密文为占位值，需替换为实际环境后方可启动。
- **测试类 `SampleUserDaoTest` 默认 `@Disabled`**：依赖本地 Docker 运行 Testcontainers，确认环境就绪后移除注解即可。

## 依赖引入

`sample` 自身不发布为依赖，仅作为可运行示例；查看其 `pom.xml` 了解应引入哪些框架模块。
