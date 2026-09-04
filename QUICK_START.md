# TravelSky Dev Framework 快速上手指南

> 15 分钟跑通一个基于 APD 框架的 CRUD 服务

---

## 1. 环境要求

| 组件 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+（推荐 3.9） |
| 数据库 | MySQL 8.x（示例使用）/ 或 PostgreSQL、Oracle（由 ORM 决定） |
| 可选 | Docker（用于 `test` 模块的 Testcontainers） |

---

## 2. 引入依赖

在业务项目的 `pom.xml` 中按需引入框架模块（版本由 `revision` 统一管理）：

```xml
<properties>
    <revision>1.0.0</revision>   <!-- 与父工程 ${revision} 对齐 -->
</properties>

<dependencies>
    <!-- Web 层：统一响应、CRUD 基类、TraceId、Long 精度 -->
    <dependency>
        <groupId>com.travelsky.base</groupId>
        <artifactId>travelsky_dev_web</artifactId>
        <version>${revision}</version>
    </dependency>

    <!-- 持久化：二选一 -->
    <!-- MyBatis-Plus 路线 -->
    <dependency>
        <groupId>com.travelsky.base</groupId>
        <artifactId>travelsky_dev_mybatis_repository</artifactId>
        <version>${revision}</version>
    </dependency>
    <!-- 或 JPA 路线 -->
    <!--
    <dependency>
        <groupId>com.travelsky.base</groupId>
        <artifactId>travelsky_dev_jpa_repository</artifactId>
        <version>${revision}</version>
    </dependency>
    -->

    <!-- 安全（可选） -->
    <dependency>
        <groupId>com.travelsky.base</groupId>
        <artifactId>travelsky_dev_security</artifactId>
        <version>${revision}</version>
    </dependency>
</dependencies>
```

> 引入 `web` 会自动传递 `common`；引入持久化模块会自动传递 `repository` / `utils`。无需手动引基础层。

---

## 3. 最小可运行示例（MyBatis-Plus 路线）

### 3.1 实体（继承审计基类）

```java
@Data
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

### 3.2 VO

```java
@Data
public class SampleUserVO implements Serializable {
    private Long id;
    private String username;
}
```

### 3.3 Mapper / Service

```java
@Mapper
public interface SampleUserMapper extends BaseMapper<SampleUserEntity> {}

@Service
public class SampleUserService
        extends AbstractBaseServiceImpl<SampleUserEntity, Long, SampleUserMapper> {
    public SampleUserService(SampleUserMapper mapper) { super(mapper); }
}
```

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

启动后自动暴露：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/sampleUser/{id}` | 查询单条 |
| POST | `/sampleUser` | 新增 |
| PUT | `/sampleUser/{id}` | 更新 |
| DELETE | `/sampleUser/{id}` | 删除 |
| POST | `/sampleUser/findAll` | 分页查询（接收 `DevQueryVO`） |

---

## 4. 配置示例

### 4.1 数据源（`application.yml`）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo?useSSL=false&serverTimezone=UTC
    username: root
    password: ENC(密文)        # 配合 security 模块自动解密
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
```

### 4.2 安全（`application-security.yml`）

```yaml
security:
  formLoginEnable: true
  ignoreUrls: /actuator/**,/swagger-ui/**,/doc.html
  jwt:
    randomSecret: false
    secret: ${JWT_SECRET}
    expiredTime: 7200000
```

启用安全需继承 `CustomSecurityConfiguration` 并注入 `UserDetailsService`（见 `sample/SecurityConfiguration`）。

### 4.3 线程池（`application.yml`）

```yaml
thread-pool:
  pool:
    order-process:
      corePoolSize: 8
      maxPoolSize: 16
      queueCapacity: 200
      rejectedPolicy: CALLER_RUNS_POLICY
```

---

## 5. 分页查询（前端提交）

```json
{
  "filter": "[[username, CONTAINS, 张], AND, [age, >=, 18]]",
  "sorts": "[{\"columns\":[\"createdTime\"],\"direction\":\"DESC\"}]",
  "page": 1,
  "limit": 20
}
```

完整 filter 语法见 `FILTER_USAGE_GUIDE.md`（前端版）与 `travelsky_dev_mybatis_repository/DevQueryVO用法.md`（后端版）。

---

## 6. 常用能力开启速查

| 想要的能力 | 操作 |
|-----------|------|
| Swagger 文档 | `swagger.web.enabled: true` |
| 监控指标 | 引入 `travelsky_dev_monitor` + `management.endpoints.web.exposure.include: health,metrics,prometheus` |
| 分布式缓存 | 引入 `travelsky_dev_cache` + 配置 `spring.hazelcast.cache.*` |
| 多数据源 | 引入 `travelsky_dev_dynamic_datasource` + `dynamic.datasource.enabled: true` |
| 去中心化发现 | 引入 `travelsky_dev_decentralization_discovery` + `decentralization.discovery.enable: true` |
| 异步/定时 | 引入 `travelsky_dev_core` + `@Import(AbstractAsyncScheduleConfiguration.class)` + `@EnableAsync` |
| 数据权限 | 在 Service 方法上加 `@DataPermission(dept = true)` |
| 测试容器 | 引入 `travelsky_dev_test`(`test` scope) + `@Testcontainers` |

---

## 7. 代码生成

`code-generator` 可基于 MySQL 表批量生成全栈 CRUD 骨架：

```bash
# 修改 code-generator.groovy 顶部的连接信息与输出目录后运行
groovy code-generator.groovy
```

> ⚠️ 生成脚本中数据库地址/密码为硬编码明文，上线前请改为环境变量/参数。

---

## 8. 运行示例项目

```bash
mvn -pl sample -am spring-boot:run
curl -X POST http://localhost:8080/sampleUser \
     -H 'Content-Type: application/json' \
     -d '{"username":"alice","password":"123"}'
```

---

## 9. 常见问题（FAQ）

**Q1：引入 `web` 后接口返回被包了一层 `ResultVO`？**
A：这是预期的统一响应包装（`ControllerResponseAdvice`）。业务直接 `return` 实体/VO 即可，框架自动包装为 `{statusCode, payload, ...}`。

**Q2：前端 Long id 变成字符串？**
A：正常。`utils` 的 `NumberSerializer` 将 Long 转字符串避免 JS 精度丢失，Web 层写回时再转回 Long。

**Q3：加了 `cache` 模块但没配 Hazelcast 也能启动？**
A：Hazelcast 与 Caffeine 均为条件装配（依赖存在才注册）。若仅引入缓存 starter 而无对应依赖，不会强制建实例。

**Q4：Spring Cloud 版本报警告？**
A：框架使用 Spring Cloud 3.1.3，与 Spring Boot 3.5.8 存在版本落差，建议对齐到匹配的 Spring Cloud 版本（已知风险 R5）。

**Q5：JWT 密钥怎么配？**
A：`security.jwt.randomSecret: true` 每次启动随机生成（开发方便）；生产建议 `security.jwt.randomSecret: false` + 固定 `security.jwt.secret`，并设置 `expiredTime`。

---

*相关文档：`FRAMEWORK_DESIGN.md`（架构设计）、各模块 `README.md`（配置项与示例）、`FILTER_USAGE_GUIDE.md`（查询 DSL）*
