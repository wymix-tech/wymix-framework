# sample 示例应用

> 框架能力的端到端可运行示例：引入框架全部模块，并给每个模块配一套可调用的示例端点。

## 模块说明

`sample` 是一个完整的 Spring Boot 应用，把框架各模块串联起来作为「用法示范」。除原有的 Entity/VO/Service/Controller CRUD 示例（`SampleUser*`）外，
`com.swcares.sample.demo` 下按模块划分了示例包，每个模块都有对应的 Controller 与配置。

### 源码目录与 ORM 选择

| 目录 | 说明 |
|------|------|
| `src/main/java` | 与 ORM 无关的公共示例（模块示例控制器、配置、安全等） |
| `src/main/mybatis/java` | MyBatis-Plus 版 Entity / Dao / Service / 装配（`mybatis` profile 默认启用） |
| `src/main/jpa/java` | JPA 版 Entity / Dao / Service / 装配（`jpa` profile），与上者**同类同名**，二选一参与编译 |

两套 ORM 代码对外暴露完全相同的 `com.swcares.sample.entity/dao/service` 契约，因此上层示例无需改动；
目录由 `pom.xml` 的 `orm.source.dir` 属性按 profile 切换（`build-helper-maven-plugin#add-source`）。

## 模块 → 示例对照

| 模块 | 示例入口 | 演示点 |
|------|----------|--------|
| `travelsky_dev_core` | `demo/core/*` · `DemoMessageSender` · `DemoAsyncTask` · `DemoScheduledTask` | `ApplicationContextHolder` 静态取 Bean / 按类型收集 SPI、`MessageService` 消息门面 + `MessageSenderService` SPI、`@Async` 异步、`@Scheduled` 调度 |
| `travelsky_dev_common` | `demo/common/*` | `ResultVO` 统一响应与字段级错误、`DevQueryVO` 过滤 DSL、`Page` 分页与 `Page#map`、`UserContextHolder` 登录上下文、`BusinessException` / `LoginException` / `SystemConstant` |
| `travelsky_dev_utils` | `demo/utils/*` | `JsonUtils`、`GlobalIdGenerator`、`StringUtils`、`DateUtils` / `LocalDateTimeUtils`、`BeanUtils`、`CompressUtil`、`UUID`、`NetUtils` / `HttpUtils`、`ReflectUtils`、`SafeConcurrentHashMap` |
| `travelsky_dev_web` | `demo/web/*` | `ControllerResponseAdvice` 自动包装响应、`MessageResourceHelper` 国际化、`RequestContextHolder` + TraceId 链路 ID、Bean Validation 参数校验、`SlowRequestAspect` 慢请求、`@RepeatRequest` 防重提交 |
| `travelsky_dev_security` | `demo/security/*` · `secure/*` · `SecurityConfiguration` | JWT 签发/校验、表单登录、`PasswordEncoder`、受保护端点（`/secure/**` 不在白名单）、登出与黑名单 |
| `travelsky_dev_cache` | `demo/cache/*` · `CacheDemoService` | `@Cacheable`/`@CachePut`/`@CacheEvict`（sample 选用 Caffeine） |
| `travelsky_dev_threadpool` | `demo/threadpool/*` | 按 `thread-pool.*` 自动注册命名线程池（普通池 + 调度池）、`ExecutorWrapper` 运行期观测与动态调参、`ThreadPoolHolder` |
| `travelsky_dev_mybatis_repository` | `SampleUserDao` / `SampleUserService` / `SampleUserController` · `demo/repository/*` · `demo/changelog/*` | `BaseRepository` CRUD、`AbstractBaseServiceImpl` 全量 API、`PageHelper` 分页、`@DataPermission` + `PermissionSqlInjector` 数据权限、`@DataChangeLog` 变更日志 |
| `travelsky_dev_jpa_repository` | `src/main/jpa/java`（`-P jpa`） | JPA 版 Entity / `JpaRepository` / `@EnableJpaRepositories` / `@EntityScan`，与 MyBatis 侧同一套 `BaseService` 契约 |
| `travelsky_dev_repository` | `SampleUserEntity` · `DataSourceConfiguration` | `AbstractAuditEntity` 审计字段自动填充、`DataChangeLogEventListener` 覆写、`DevQueryVO` 操作符速查 |
| `travelsky_dev_data_migration` | `demo/dataMigration/*` · `DemoDataMigrationListener` | `@DataMigration` 实体、`DataChangeLogEvent` 监听与迁移回调统计 |
| `travelsky_dev_dynamic_datasource` | `demo/dynamicDatasource/*` · `DynamicDatasourceDemoService` | `@DynamicDatasource` 注解切库、`DynamicDataSourceContextHolder` 编程式切库 |
| `travelsky_dev_decentralization_discovery` | `demo/discovery/*` | 节点信息查询（需 `discovery` profile，见「已知限制」） |
| `travelsky_dev_monitor` | `demo/monitor/*` · `MonitorApplicationStartedEventListener`（自动注册） | Actuator/Prometheus 默认值注入、`/actuator/**` 放行、开放端点清单查询 |
| `travelsky_dev_test` | `src/test/java/.../SampleMySqlContainer`、`SampleUserDaoTest` | Testcontainers 容器基类 + `@DataSet` 数据编排 |
| `travelsky_dev_ai` | — | 空壳占位模块，暂无自动装配内容，仅保留坐标 |

## 配置文件

| 文件 | 说明 |
|------|------|
| `application.yml` | 主配置：激活的 profile、各模块开关、i18n、慢请求阈值、Swagger |
| `application-datasource.yml` | 数据源 |
| `application-security.yml` | 安全（JWT / 白名单 / 表单登录） |
| `application-cache.yml` | Caffeine 缓存规格 |
| `application-async.yml` | 异步线程池 + `thread-pool.*` 自定义线程池（含示例用的 `testThreadPool` / `testScheduledThreadPool`） |
| `application-monitor.yml` | Actuator 暴露端点与应用信息 |
| `application-discovery.yml` | 去中心化服务发现（opt-in） |
| `application-dynamic-datasource.yml` | 动态数据源（opt-in） |
| `messages*.properties` | i18n 文案（`MessageResourceHelper` 与全局异常文案渲染） |
| `i18n/validation/ValidationMessages*.properties` | Bean Validation 文案（基名 `spring.validate.messages`） |

## 示例端点速查

`/demo/**` 已在 `security.ignoreUrls` 放行，可直接访问。

```bash
# utils：JsonUtils / 雪花 ID / 字符串 / 日期 / 压缩 / UUID / 网络 / 反射 / 并发 Map / Bean 拷贝
curl http://localhost:8080/demo/utils/json
curl http://localhost:8080/demo/utils/id
curl http://localhost:8080/demo/utils/compress
curl http://localhost:8080/demo/utils/reflect

# common：统一响应 / 查询 DSL / 登录上下文 / 异常体系 / 错误码
curl http://localhost:8080/demo/common/result
curl 'http://localhost:8080/demo/common/query?username=admin&page=1&limit=10'
curl -X POST 'http://localhost:8080/demo/common/principal?username=admin'
curl http://localhost:8080/demo/common/exception/login

# core：上下文 / 消息门面 / 异步 / 调度
curl http://localhost:8080/demo/core/contextHolder
curl -X POST 'http://localhost:8080/demo/core/message?content=hi'
curl http://localhost:8080/demo/core/scheduled

# repository：BaseService 全量 API + DSL 速查
curl http://localhost:8080/demo/repository/operators
curl 'http://localhost:8080/demo/repository/page?username=admin&page=1&limit=10'
curl http://localhost:8080/demo/repository/permission

# 数据变更日志 / 数据迁移
curl http://localhost:8080/demo/changelog/entities
curl http://localhost:8080/demo/dataMigration/stats

# web：i18n / 链路 ID / 参数校验 / 慢请求 / 防重提交
curl -H 'Accept-Language: zh-CN' http://localhost:8080/demo/web/i18n
curl http://localhost:8080/demo/web/requestContext
curl -X POST -H 'Content-Type: application/json' -d '{"name":"","age":0}' http://localhost:8080/demo/web/validate
curl 'http://localhost:8080/demo/web/slow?millis=5000'

# cache / threadpool
curl http://localhost:8080/demo/cache/k1
curl http://localhost:8080/demo/threadpool/status
curl -X PUT 'http://localhost:8080/demo/threadpool/resize?corePoolSize=8&maxPoolSize=16'

# security / monitor
curl -X POST 'http://localhost:8080/demo/security/token?username=admin'
curl http://localhost:8080/demo/monitor/endpoints
curl http://localhost:8080/actuator/health

# 受保护端点（不在白名单，需先登录）
curl -i http://localhost:8080/secure/me
curl -i -X POST -d 'username=admin&password=Abc@123' -c cookie.txt http://localhost:8080/login
curl -b cookie.txt http://localhost:8080/secure/me
```

Swagger UI：`http://localhost:8080/doc.html`（由 web 模块 `ApiDocAutoConfiguration` 自动装配）。

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
# 构建并运行（MyBatis 版，默认）
mvn -f sample/pom.xml clean package -DskipTests
java -jar sample/target/sample-2.0-SNAPSHOT.jar

# 切换到 JPA ORM
mvn -f sample/pom.xml clean package -DskipTests -P jpa -P '!mybatis'
```

## 已知限制

- **`decentralization_discovery` 暂不可直接启用**：`NodeService` 依赖 Spring Cloud OpenFeign 的 `FeignContext`，
  但父 POM 锁定的 `spring-cloud-starter-openfeign` 为 `3.1.3`（Spring Cloud 2021.x，仅提供 Boot 2 时代的 `spring.factories` 自动装配）。
  在 Boot 3.5 下 `FeignAutoConfiguration` 不会被加载，开启 `decentralization.discovery.enable=true` 会报
  `required a bean of type 'FeignContext' that could not be found`。
  需将 Spring Cloud 升级到与 Boot 3.5 匹配的 2025.x 版本后方可启用；示例 `demo/discovery/*` 已按同一开关条件装配，届时自动生效。
  因此 `rpc/Test1Api`、`Test2Api` 目前仅保留接口定义，未注入到任何 Controller。
- **`dynamicDatasource` profile 需要真实数据库**：配置中的 MySQL 地址与 `ENC()` 密文为占位值，需替换为实际环境后方可启动。
- **数据权限能力仅 MyBatis 侧提供**：`@DataPermission` / `PermissionSqlInjector` 位于 `travelsky_dev_mybatis_repository`，
  JPA 版 `SampleUserService#findAllWithDataPermission` 仅返回全量数据。
- **测试类 `SampleUserDaoTest` 默认 `@Disabled`**：依赖本地 Docker 运行 Testcontainers，确认环境就绪后移除注解即可。

## 依赖引入

`sample` 自身不发布为依赖，仅作为可运行示例；查看其 `pom.xml` 了解应引入哪些框架模块。
