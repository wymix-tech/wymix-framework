# jpa_repository JPA 数据访问模块

> 基于 Spring Data JPA 的持久化实现：审计字段自动填充、数据变更日志与统一查询 DSL。

## 模块说明

`jpa_repository` 在 Spring Data JPA 之上提供开箱即用的企业能力：继承 `AbstractAuditEntity` 的实体自动填充创建/修改人及时间；通过 `BaseRepository` + `AbstractBaseServiceImpl` 提供标准化 CRUD 服务；`SpecificationHelper` 将 `DevQueryVO` 的 filter DSL 解析为 JPA `Specification`。

## 依赖引入

```xml
<dependency>
    <groupId>org.wymix.framework</groupId>
    <artifactId>jpa_repository</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `wymix.jpa.enabled` | 是否启用 JPA 基础装配（审计填充） | `true` |
| `wymix.jpa.data-permission.enabled` | 是否注册数据权限切面 `DataPermissionAspect`（`@DataPermission` 入口）；同时引入 MyBatis 侧 `@EnableDataPermission` 时建议设为 `false`，只保留一个切面实例 | `true` |
| `data-changelog.enabled` | 是否启用数据变更日志 | `true` |
| `data.changelog.basePackages` | 变更日志注解扫描包（逗号分隔） | — |
| `spring.jpa.*` | 标准 Spring Data JPA 配置 | — |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `JpaConfiguration` | 基础装配：`DatabaseAuditorAware`（审计人）、`AuditHandler`、`AuditListener`（由 `@Import` 注册）、`DataPermissionAspect`（受 `wymix.jpa.data-permission.enabled` 控制） |
| `DataChangeLogConfiguration` | 变更日志装配：Hibernate 事件监听器挂载、请求拦截器、`ManualDmlAspect`、`BatchDeleteAspect` |
| `SpecificationHelper` / `PageHelper` | `DevQueryVO` → `Specification` 与分页转换 |

## 继承类（业务方核心用法）

| 类/接口 | 说明 |
|---------|------|
| `AbstractAuditEntity` | **实体基类**：继承后自动维护 `createdBy`/`createdTime`/`lastModifiedBy`/`lastModifiedTime` |
| `BaseRepository<T, ID>` | **仓储基类**：扩展 `JpaRepository` + `JpaSpecificationExecutor` |
| `AbstractBaseServiceImpl<T, ID, REPOSITORY>` | **服务基类**：实现 `BaseService`，内置钩子 `beforeSave`/`afterSave`/`beforeUpdate(old, cur)`/`afterUpdate` |

## 数据权限（`@DataPermission`）

JPA 侧与 MyBatis 侧共用 `repository` 中上提的注解 / 上下文 / 切面；区别只在「条件怎么落到查询上」：MyBatis 由插件改写 SQL，JPA 由 `SpecificationHelper#applyDataPermission` 追加 `Predicate`。

```java
// 1) 注入器：JPA 场景必须注册为 Spring Bean（标注 @Component 或声明 @Bean）
//    —— 与 MyBatis 不同，JPA 侧没有 @EnableDataPermission 扫描注册器
@Component
public class OwnerSqlInject implements PermissionSqlInjector {
    @Override public Map<String, Object> additionalParameters() { return Map.of("owner", "ALICE"); }
    @Override public String injectSql() { return "OWNER = :owner"; }   // 列名或属性名均可
    @Override public InjectType type() { return InjectType.WHERE_ADDITIONAL; }
}

// 2) 在 Service 方法上声明：findAll() / findAll(DevQueryVO) 均会带上该条件
@DataPermission(injectors = {OwnerSqlInject.class})
public List<OrderEntity> findAllWithPermission() { return this.findAll(); }
```

> **限制与约定（务必阅读）**
>
> - **注入器必须是 Bean**：JPA 场景无 `@EnableDataPermission` 注册器，`ApplicationContextHolder.getBean(...)` 取不到时会退化为反射实例化并 `warn`（`@Autowired` 字段不会注入）；无法实例化则抛出带类名的明确异常。
> - **`injectSql` 仅支持 `=` 条件**：可用 `AND` 连接多条（`DEPT_ID = :deptId AND STATUS = 1`），值支持 `:param` 命名参数与字面量。不支持 `OR` / `IN` / `LIKE` / `>` 等，遇到会**快速失败**（宁可报错，也不静默漏掉权限条件）。
> - **`statementIds` 不支持按语句过滤**：JPA 没有 MappedStatement id 概念，仅当白名单为空或含 `"ALL"` 时生效；显式配置了具体 statementId 时会跳过并 `warn`。
> - **`REPLACE_TABLE_NAME` 不适用**：依赖 SQL 文本改写，JPA 侧直接跳过。
> - **异步场景**：上下文用 `TransmittableThreadLocal`，只有经 `TtlExecutors` / `TtlRunnable` 包装的线程池才透传；框架 `TaskDecorator` 未做 TTL 包装，`@Async` 中需重新标注 `@DataPermission`。

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `beforeSave` / `afterSave` / `beforeUpdate` / `afterUpdate` | 服务基类生命周期钩子，覆写即可插入业务逻辑 |
| `DataChangeLogEventListener` Bean | 重写 `changelog(DataChangeLogDTO)` 自定义变更落地；模块以 `@ConditionalOnMissingBean` 注册了默认实现，声明自定义 `DataChangeLogEventListener` Bean 即不装配默认实现（该 Bean 由 `@Bean` 方法显式注册，并非类路径条件装配） |
| `ManualDmlContext` | 自定义 SQL 场景手动触发变更记录 |
| `AuditListener` / `DatabaseAuditorAware` | 审计填充逻辑可按 Spring Data JPA 标准方式定制 |

## 使用示例

```java
// 1) 实体继承审计基类（自动填充审计字段）
@Data
@Entity
@Table(name = "EMPLOYEE")
public class EmployeeEntity extends AbstractAuditEntity {
    @Id @GeneratedValue
    private Long id;
    private String name;
}

// 2) 仓储继承 BaseRepository
public interface EmployeeRepository extends BaseRepository<EmployeeEntity, Long> {}

// 3) 服务继承 AbstractBaseServiceImpl
@Service
public class EmployeeService extends AbstractBaseServiceImpl<EmployeeEntity, Long, EmployeeRepository> {
    public EmployeeService(EmployeeRepository repo) { super(repo); }

    @Override
    protected void beforeSave(EmployeeEntity e) { /* 保存前钩子 */ }
}

// 4) 使用 DevQueryVO 查询 DSL
DevQueryVO q = new DevQueryVO();
q.setFilter("[[name, CONTAINS, 张], AND, [age, >=, 18]]");
q.setSorts("[{\"columns\":[\"createdTime\"],\"direction\":\"DESC\"}]");
Page<EmployeeEntity> page = service.findAll(q);
```

> **包路径说明（3.x 起）**：业务方直接使用的基类（`BaseRepository`、`AbstractBaseServiceImpl`、`AbstractAuditEntity`）位于 `org.wymix.framework.repository.jpa.*` 子包；旧包路径 `org.wymix.framework.repository.api`、`...audit.domain` 下的兼容别名已随包路径迁移一并移除，请直接使用下列 import：
>
> ```java
> import org.wymix.framework.repository.jpa.api.BaseRepository;
> import org.wymix.framework.repository.jpa.api.AbstractBaseServiceImpl;
> import org.wymix.framework.repository.jpa.audit.domain.AbstractAuditEntity;
> ```
