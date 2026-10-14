# Wymix Dev Framework 快速上手指南

> 15 分钟跑通一个基于本框架的 CRUD 服务

> **事实基准**：本文出现冲突时，以各模块 `README.md` 与模块源码为准。当前版本：`revision=3.0.0-SNAPSHOT`，
> 父 POM `dev_parent:3.0.0-SNAPSHOT`，Spring Boot `3.5.8`，Java 17。

---

## 1. 环境要求

| 组件 | 版本要求 |
|------|----------|
| JDK | 17 |
| Maven | 3.8+（推荐 3.9） |
| 数据库 | MySQL 8.x（示例使用）/ 或 PostgreSQL、Oracle（由 ORM 决定） |
| 可选 | Docker（`test` 模块的 Testcontainers 需要可用 Docker 环境） |

---

## 2. 引入依赖

在业务项目的 `pom.xml` 中按需引入框架模块（版本由 `revision` 统一管理，当前父 Pom 的 `<revision>` 为 `3.0.0-SNAPSHOT`）：

```xml
<properties>
    <revision>3.0.0-SNAPSHOT</revision>   <!-- 与框架父工程 ${revision} 对齐 -->
</properties>

<dependencies>
    <!-- Web 层：统一响应、CRUD 基类、TraceId、Long 精度、i18n、慢请求 -->
    <dependency>
        <groupId>org.wymix.framework</groupId>
        <artifactId>web</artifactId>
        <version>${revision}</version>
    </dependency>

    <!-- 持久化：二选一 -->
    <!-- MyBatis-Plus 路线 -->
    <dependency>
        <groupId>org.wymix.framework</groupId>
        <artifactId>mybatis_repository</artifactId>
        <version>${revision}</version>
    </dependency>
    <!-- 或 JPA 路线 -->
    <!--
    <dependency>
        <groupId>org.wymix.framework</groupId>
        <artifactId>jpa_repository</artifactId>
        <version>${revision}</version>
    </dependency>
    -->

    <!-- 安全（可选） -->
    <dependency>
        <groupId>org.wymix.framework</groupId>
        <artifactId>security</artifactId>
        <version>${revision}</version>
    </dependency>
</dependencies>
```

> 引入 `web` 会自动传递 `security` 与 `core`（`web` 的 pom 直接 compile 依赖 `security`，后者再带入其余基础模块）；
> 因此**仅引入 `web` 即会激活安全装配**（默认 `matchIfMissing = true`），若暂不需要认证，请显式配置 `security.enabled: false`。
> 引入持久化模块会自动传递 `repository` / `common` / `utils`。无需手动引基础层。

---

## 3. 最小可运行示例（MyBatis-Plus 路线）

> 完整可运行版本见 `sample`（`src/main/mybatis/java` 下的 `SampleUser*`）。

### 3.1 实体（继承审计基类）

```java
import org.wymix.framework.repository.mybatis.audit.domain.AbstractAuditEntity;

@Data
@EqualsAndHashCode(callSuper = true)   // 继承基类时建议显式 callSuper = true
@TableName("SAMPLE_USER")
public class SampleUserEntity extends AbstractAuditEntity implements Serializable {
    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("USERNAME")
    private String username;

    @TableField("PASSWORD")
    private String password;
}
```

> 审计字段（createdBy/createdTime/lastModifiedBy/lastModifiedTime）由
> `AbstractAuditEntity` + `AuditFieldFillHandler` 自动填充。
> `@DataChangeLog` 可按需叠加，用于记录变更日志（见 `repository` README）。

### 3.2 VO

```java
// 也可继承 AbstractAuditVO（common 模块）以携带审计字段
@Data
public class SampleUserVO implements Serializable {
    private Long id;
    private String username;
}
```

### 3.3 Dao / Service

```java
import org.wymix.framework.repository.mybatis.api.BaseRepository;

// Dao：继承框架 BaseRepository（本身扩展自 MyBatis-Plus BaseMapper），无需再写 @Mapper 之外的代码
// （@Mapper 或 @MapperScan 扫描二者必须满足其一，sample 中由 DataSourceConfiguration 的 @MapperScan 处理）
public interface SampleUserMapper extends BaseRepository<SampleUserEntity, Long> {}

@Service
public class SampleUserService
        extends AbstractBaseServiceImpl<SampleUserEntity, Long, SampleUserMapper> {
    public SampleUserService(SampleUserMapper mapper) { super(mapper); }
}
```

> `3.x` 起业务方基类位于 `org.wymix.framework.repository.mybatis.*` 子包，旧包路径
> `org.wymix.framework.repository.api` 已移除。

### 3.4 Controller（继承即获得标准 CRUD 端点）

```java
@RestController
@RequestMapping("sampleUser")
public class SampleUserController
        extends AbstractBaseController<SampleUserEntity, SampleUserVO, Long, SampleUserService>
        implements BaseController<SampleUserEntity, SampleUserVO, Long> {

    public SampleUserController(SampleUserService service) { super(service); }
}
```

启动后自动暴露（`{id}` 为 Long 型主键）：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/sampleUser/{id}` | 查询单条 |
| GET | `/sampleUser` | 分页 + filter 查询（接收 `DevQueryVO` 平铺参数） |
| POST | `/sampleUser` | 新增 |
| PUT | `/sampleUser/{id}` | 更新 |
| DELETE | `/sampleUser/{id}` | 删除 |
| POST | `/sampleUser/batch` | 批量新增 |
| PUT | `/sampleUser/batch` | 批量更新 |
| DELETE | `/sampleUser/batch` | 批量删除 |

> ⚠️ **与旧文档的差异**：分页查询是 `GET /sampleUser`（无 `findAll` 后缀），且 `DevQueryVO` 无 `@RequestBody`，
> 需以 query 参数形式提交（`?filter=...&sorts=...&page=1&limit=20`）；批量端点走 `/batch` 子路径。

---

## 4. 配置示例

### 4.1 数据源（`application.yml`）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo?useSSL=false&serverTimezone=UTC
    username: root
    password: ENC(密文)        # 引入 security 模块后由 DecryptEnvironmentPostProcessor 自动解密
  # JPA 路线才需要下列配置
  jpa:
    hibernate:
      ddl-auto: update
# MyBatis-Plus 路线可选：指定 XML 位置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
```

> 框架已通过 `MybatisPlusPropertiesCustomizer` 默认设置 `mapUnderscoreToCamelCase=true`、
> `jdbcTypeForNull=NULL`，并把 ID 生成接入 `GlobalIdGenerator`（雪花 ID），无需重复配置。

### 4.2 安全（`application-security.yml`）

```yaml
security:
  enabled: true            # 默认 true；不需要认证时置 false
  formLoginEnable: true
  loginPage: /login
  loginProcessingUrl: /login
  ignoreUrls: /actuator/**,/swagger-ui/**,/doc.html
  jwt:
    randomSecret: false
    secret: ${JWT_SECRET}  # 建议 32 位长度以上
    expiredTime: 86400     # 单位：秒（示例配置）
```

启用安全：引入 `security`（`web` 已传递引入）后，提供
`UserDetailsService` Bean 即自动生效，**无需继承任何配置类**
（见 `sample/src/main/java/com/swcares/sample/configuration/SecurityConfiguration`）。
可通过声明 `SecurityCustomizer` Bean 定制 `HttpSecurity`。

```java
@Configuration
public class SecurityConfiguration {
    private final PasswordEncoder passwordEncoder;   // 由 security 模块自动装配（BCrypt）
    public SecurityConfiguration(PasswordEncoder passwordEncoder) { this.passwordEncoder = passwordEncoder; }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername("admin")
                .password("Abc@123")
                .passwordEncoder(s -> passwordEncoder.encode(s))
                .authorities("superadmin")
                .build());
        return manager;
    }
}
```

> 示例还应放行 `/error`：任何 404 转发到 `/error` 时若不放行会被鉴权拦截，
> 表现为「白名单路径却返回 401」。做法见 `sample` 的 `SampleWebConfiguration`（声明 `IgnoreUrlContext` Bean）。

### 4.3 线程池（`application.yml`）

```yaml
wymix:
  threadpool:
    enabled: true      # 默认 true
thread-pool:
  pool:
    order-process:
      corePoolSize: 8
      maxPoolSize: 16
      queueCapacity: 200
      rejectedPolicy: CALLER_RUNS_POLICY
```

> 每个 `pool.<name>` 会以 `<name>` 为 Bean 名注册单例，可 `@Qualifier("order-process")` 注入 `ExecutorWrapper`。

### 4.4 异步（`application.yml`）

```yaml
async:
  enabled: true            # 默认 true，由 core 模块提供 @Async / @Scheduled 线程池
  corePoolSize: 4
  maxPoolSize: 8
  queueCapacity: 300
```

---

## 5. 分页查询

`DevQueryVO` 以**平铺 query 参数**提交（GET `/sampleUser`）：

```bash
curl -G 'http://localhost:8080/sampleUser' \
  --data-urlencode 'filter=[[username, CONTAINS, 张], AND, [age, >=, 18]]' \
  --data-urlencode 'sorts=[{"columns":["createdTime"],"direction":"DESC"}]' \
  --data-urlencode 'page=1' \
  --data-urlencode 'limit=20'
```

等价的请求体结构（后端接收模型，也可由前端拼成 JSON 后按后端需要提交）：

```json
{
  "filter": "[[username, CONTAINS, 张], AND, [age, >=, 18]]",
  "sorts": "[{\"columns\":[\"createdTime\"],\"direction\":\"DESC\"}]",
  "page": 1,
  "limit": 20
}
```

| 字段 | 类型 | 默认值 |
|------|------|--------|
| `filter` | String（filter DSL 字符串） | 空（不过滤） |
| `sorts` | String（JSON 数组字符串） | 空（不排序） |
| `page` | Long | 1 |
| `limit` | Long | 20（单页上限由分页插件限制为 10000） |

完整 filter 语法见根目录 `FILTER_USAGE_GUIDE.md`（前端版）与
`mybatis_repository/DevQueryVO用法.md`（后端版）。

---

## 6. 常用能力开启速查

| 想要的能力 | 操作 |
|-----------|------|
| Swagger 文档 | 引入 `web` 即自动装配（`swagger.enabled: false` 关闭）；放行 UI 路径加 `swagger.web.enabled: true`，UI 默认路径 `/doc.html` |
| 监控指标 | 引入 `monitor`（自动放行 `/actuator/**` 并注入 Prometheus 默认值，开关 `wymix.monitor.enabled`） |
| 本地缓存 | 引入 `cache` **且自行引入 `caffeine` 依赖**（模块中该依赖为 `optional`，不传递）+ 按需配置 `caffeine.cache.<name>.*` |
| 多数据源 | 引入 `dynamic_datasource` + 自行引入 JDBC 驱动与 HikariCP + 配置 `dynamic.datasource.enabled: true` 与 `primary`、`items` |
| 去中心化发现 | 引入 `decentralization_discovery` + 配置 `decentralization.discovery.enable: true` 与 `service.<svc>` 地址列表（无可选依赖，不做自动发现） |
| 异步/定时 | 引入 `core` 即自动生效（`async.enabled=false` 可关闭异步/调度装配，`wymix.core.enabled=false` 关闭基础装配） |
| 数据权限 | MyBatis 侧：`@EnableDataPermission(basePackages=...)` + Service 方法加 `@DataPermission(injectors = {XxxInjector.class})`（JPA 侧不提供） |
| 变更日志 / 迁移 | 实体加 `@DataChangeLog`（可选 `@DataMigration`）+ `data.changelog.basePackages` + 引入 `data_migration` |
| 测试容器 | 引入 `test`（`test` scope）+ 继承 `AbstractMySqlContainer` 等基类 |
| 防重复提交 | 自行声明 `RepeatRequestAspect` Bean，再在 Controller 方法上加 `@RepeatRequest`（框架默认未注册该切面，见 FAQ Q6） |

---

## 7. 代码生成

`code-generator` 可基于 MySQL 表批量生成全栈 CRUD 骨架。**数据库凭证已改为环境变量/系统属性注入，脚本不再内置明文**：

```bash
export GEN_DB_URL="jdbc:mysql://127.0.0.1:3306/your_db?useSSL=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8"
export GEN_DB_USER="your_user"
export GEN_DB_PASSWORD="your_password"

# 脚本位于 code-generator/src/main/groovy/code-generator.groovy
groovy code-generator/src/main/groovy/code-generator.groovy
```

> ⚠️ 生成脚本仍留有硬编码的**包名前缀**（`com.swcares.ttimas`）与**输出目录**（默认 Windows 路径），
> 运行前请在脚本内按目标工程修改；缺失任一凭证变量脚本会打印提示并 `System.exit(1)` 退出。

---

## 8. 运行示例项目

```bash
# MyBatis 版（默认 profile）
mvn -f sample/pom.xml clean package -DskipTests
java -jar sample/target/sample-2.0-SNAPSHOT.jar

# 或用 Maven 直接跑（需先构建依赖模块）
mvn -pl sample -am spring-boot:run

# 切换到 JPA 版
mvn -f sample/pom.xml clean package -DskipTests -P jpa -P '!mybatis'
```

调用示例（新增）：

```bash
curl -X POST http://localhost:8080/sampleUser \
     -H 'Content-Type: application/json' \
     -d '{"username":"alice","password":"123"}'
```

> 响应体为 `ResultVO` 统一包装：`{"statusCode":200,"payload":{...},"errorMessage":null,"invalidMessage":{}}`。

---

## 9. 常见问题（FAQ）

**Q1：引入 `web` 后接口返回被包了一层 `ResultVO`？**
A：这是预期的统一响应包装（`ControllerResponseAdvice`，实现 `ResponseBodyAdvice`）。业务直接 `return` 实体/VO 即可，框架自动包装为
`{statusCode, payload, errorMessage, invalidMessage}`。已返回 `ResultVO` 的方法不会被二次包装；
`springdoc` / `actuator` 包下的返回值也会跳过包装。

**Q2：前端 Long id 什么时候变成字符串？**
A：**数值超过 `100000000000000`（1e14）才转字符串**（`utils` 的 `NumberSerializer` 阈值），未超阈值仍按数字输出。
目的是规避 JS 53 位精度丢失；雪花 ID 生成的 ID 通常超过该阈值，因此前端看到的 id 多为字符串。
注意框架**只做了序列化侧的 Long→String**，未提供配套的 String→Long 反序列化器，写入实体 ID 时需业务侧自行处理类型转换。

**Q3：加了 `cache` 模块但没引入 Caffeine 依赖能启动吗？**
A：能。`Caffeine` 为 `optional` 依赖且在装配上带 `@ConditionalOnClass(Caffeine)`，未引入时不会注册任何缓存 Bean。
本模块只提供 Caffeine 本地缓存，**不含分布式/多级缓存装配**（Hazelcast 与组合缓存已在 `b003f92` 中整体移除）；
需要分布式缓存请业务侧显式装配自己的 `CacheManager` / `CachingConfigurer`。

**Q4：Feign 客户端怎么配？Spring Cloud 版本如何选？**
A：父 POM 已改为导入 `spring-cloud-dependencies` BOM（当前 **2025.0.3**，对应 Boot 3.5.x），各 spring-cloud starter
不再单独写版本，升级 Boot 时需按官方版本矩阵同步调整 `spring-cloud.version`（Boot 3.5.x → 2025.0.x）。

原先锁定 `spring-cloud-starter-openfeign:3.1.3`（2021.x）时，其自动配置只注册在 `spring.factories` 中，Boot 3 不再读取该文件，
导致整个 openfeign 自动装配都不生效（`FeignContext` Bean 缺失）。对齐后 `@FeignClient`
与 `decentralization_discovery` 均正常工作。

**Q5：JWT 密钥怎么配？**
A：`security.jwt.randomSecret: true` 每次启动随机生成（开发方便）；生产建议 `randomSecret: false` + 固定 `security.jwt.secret`，
并设置 `security.jwt.expiredTime`（**单位：秒**，默认 0）。

**Q6：`@RepeatRequest` 加了没生效？**
A：`web` 模块中 `RepeatRequestAspect` 的 `@Bean` 注册**当前处于注释状态**（`WebConfiguration` 中已注释），
需业务方自行声明该 Bean（见 `sample` 的 `SampleWebConfiguration`）后注解才生效。
另外该切面**仅在 Controller 方法上有效**（非 Controller 只打 warn 日志不做拦截），
去重键为「类+方法+参数」且存放在 `HttpSession` 中，因此**只在同一个会话内去重**。

**Q7：为什么只引入了 `web` 就跳登录页？**
A：`web` 的 pom 直接 compile 依赖 `security`，后者默认开启（`security.enabled` `matchIfMissing = true`）。
不需要认证时显式配置 `security.enabled: false`，或提供 `UserDetailsService` Bean 并配置 `security.ignoreUrls` 放行。

---

*相关文档：`FRAMEWORK_DESIGN.md`（架构设计）、各模块 `README.md`（配置项与示例）、`FILTER_USAGE_GUIDE.md`（查询 DSL）*
