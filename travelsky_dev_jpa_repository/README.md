# travelsky_dev_jpa_repository JPA 数据访问模块

> 基于 Spring Data JPA 的持久化实现：审计字段自动填充、数据变更日志与统一查询 DSL。

## 模块说明

`travelsky_dev_jpa_repository` 在 Spring Data JPA 之上提供开箱即用的企业能力：继承 `AbstractAuditEntity` 的实体自动填充创建/修改人及时间；通过 `BaseRepository` + `AbstractBaseServiceImpl` 提供标准化 CRUD 服务；`SpecificationHelper` 将 `DevQueryVO` 的 filter DSL 解析为 JPA `Specification`。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_jpa_repository</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `travelsky.jpa.enabled` | 是否启用 JPA 基础装配（审计填充） | `true` |
| `data-changelog.enabled` | 是否启用数据变更日志 | `true` |
| `data.changelog.basePackages` | 变更日志注解扫描包（逗号分隔） | — |
| `spring.jpa.*` | 标准 Spring Data JPA 配置 | — |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `JpaConfiguration` | 基础装配：`DatabaseAuditorAware`（审计人）、`AuditHandler`、`AuditListener` |
| `DataChangeLogConfiguration` | 变更日志装配：Hibernate 事件监听器挂载、请求拦截器、`ManualDmlAspect`、`BatchDeleteAspect` |
| `SpecificationHelper` / `PageHelper` | `DevQueryVO` → `Specification` 与分页转换 |

## 继承类（业务方核心用法）

| 类/接口 | 说明 |
|---------|------|
| `AbstractAuditEntity` | **实体基类**：继承后自动维护 `createdBy`/`createdTime`/`lastModifiedBy`/`lastModifiedTime` |
| `BaseRepository<T, ID>` | **仓储基类**：扩展 `JpaRepository` + `JpaSpecificationExecutor` |
| `AbstractBaseServiceImpl<T, ID, REPOSITORY>` | **服务基类**：实现 `BaseService`，内置钩子 `beforeSave`/`afterSave`/`beforeUpdate(old, cur)`/`afterUpdate` |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `beforeSave` / `afterSave` / `beforeUpdate` / `afterUpdate` | 服务基类生命周期钩子，覆写即可插入业务逻辑 |
| `DataChangeLogEventListener` Bean | 重写 `changelog(DataChangeLogDTO)` 自定义变更落地（自动装配了默认实现，声明自定义 Bean 即覆盖） |
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

> **包路径说明（3.x 起）**：JPA 专属类已迁移到 `com.travelsky.base.repository.jpa.*` 子包，
> 与 MyBatis 实现完全隔离，两模块可并存引入。旧包路径 `com.travelsky.base.repository.api` /
> `...audit.domain` 保留 deprecated 兼容别名（`forRemoval = true`），请尽快迁移。
> 示例中的 import：
>
> ```java
> import com.travelsky.base.repository.jpa.api.BaseRepository;
> import com.travelsky.base.repository.jpa.api.AbstractBaseServiceImpl;
> import com.travelsky.base.repository.jpa.audit.domain.AbstractAuditEntity;
> ```
