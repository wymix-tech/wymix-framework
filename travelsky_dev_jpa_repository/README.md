# travelsky_dev_jpa_repository JPA 数据访问模块

> 基于 Spring Data JPA 的持久化实现：审计字段自动填充、数据变更日志与统一查询 DSL。

## 模块说明

`travelsky_dev_jpa_repository` 在 Spring Data JPA 之上提供开箱即用的企业能力：继承 `AbstractAuditEntity` 的实体自动填充创建/修改人及时间；通过 `BaseRepository` + `AbstractBaseServiceImpl` 提供标准化 CRUD 服务；`SpecificationHelper` 将 `DevQueryVO` 的 filter DSL 解析为 JPA `Specification`。

## 功能说明

| 能力 | 关键类 | 说明 |
|------|--------|------|
| 审计填充 | `AbstractAuditEntity` / `AuditListener` / `DatabaseAuditorAware` | 实体继承后自动维护 `createdBy`/`createdTime`/`lastModifiedBy`/`lastModifiedTime` |
| 基础仓储 | `BaseRepository<T, ID>` | 扩展 `JpaRepository`，提供通用查询能力 |
| 基础服务 | `AbstractBaseServiceImpl<T, ID, R>` | 实现 `BaseService`，内置 `beforeSave`/`afterSave`/`beforeUpdate`/`afterUpdate` 钩子 |
| 查询 DSL | `SpecificationHelper` / `PageHelper` | 将 `DevQueryVO.filter/sorts` 转换为 JPA `Specification` 与分页 |
| 变更日志 | `DataChangeLogConfiguration` + `DataChangeLogListener` / `ManualDmlAspect` | 拦截 DML，记录数据变更（配合 `repository` 模块注解） |
| 手动 DML | `ManualDmlContext` / `ManualDmlDataChangeInterceptor` | 在自定义 SQL 场景手动触发变更记录 |
| 批量删除 | `BatchDeleteAspect` | 拦截批量删除并补充变更日志 |

## 配置项

本模块通过 Spring Data JPA 标准配置（`spring.jpa.*`）驱动，无额外专有前缀。

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

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_jpa_repository</artifactId>
    <version>${revision}</version>
</dependency>
```
