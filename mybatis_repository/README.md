# mybatis_repository MyBatis 数据访问模块

> 基于 MyBatis-Plus 的持久化实现：分页插件、数据权限 AOP 与统一查询 DSL（`DevQueryVO`）。

## 模块说明

`mybatis_repository` 在 MyBatis-Plus 之上提供企业级数据访问能力。核心是统一的查询 DSL `DevQueryVO`：前端/调用方提交一段类 Lisp 的嵌套括号表达式（filter），后端自动解析为 SQL 条件。同时提供 `@DataPermission` 数据权限、审计字段自动填充与分页插件（`maxLimit=10000`）。

详细查询语法见同目录文档：
- [DevQueryVO用法.md](./DevQueryVO用法.md)（后端视角）
- 根目录 `FILTER_USAGE_GUIDE.md`（前端视角，含 AI 提示词模板）

## 依赖引入

```xml
<dependency>
    <groupId>org.wymix.framework</groupId>
    <artifactId>mybatis_repository</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `wymix.mybatis.enabled` | 是否启用 MyBatis 基础装配（分页/审计/ID 生成） | `true` |
| `data-changelog.enabled` | 是否启用数据变更日志 | `true` |
| `data.changelog.basePackages` | 变更日志注解扫描包（逗号分隔），同时扫描 `@DataChangeLog` 与 `@DataMigration` 实体 | — |
| `mybatis-plus.*` | 标准 MyBatis-Plus 配置 | — |

> 框架已通过 `MybatisPlusPropertiesCustomizer` 默认设置：`mapUnderscoreToCamelCase=true`、`jdbcTypeForNull=NULL`、ID 生成接入 `GlobalIdGenerator`（雪花 ID）。

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `MyBatisConfiguration` | 分页插件、`AuditFieldFillHandler`（审计填充）、`IdentifierGenerator`（雪花 ID）、驼峰映射 |
| `DataChangeLogConfiguration` | 变更日志装配：`DataChangeLogPlugin` + `DataChangeLogTableRegistry`（扫描 `@DataChangeLog` 实体） |
| `QueryParseHelper` / `PageHelper` | `DevQueryVO` filter/sorts → SQL 条件与分页 |

## 继承类（业务方核心用法）

| 类/接口 | 说明 |
|---------|------|
| `AbstractAuditEntity` | **实体基类**：继承后自动填充 `createdBy`/`createdTime`/`lastModifiedBy`/`lastModifiedTime` |
| `BaseRepository<T, ID>` | **Mapper 基类**：扩展 MyBatis-Plus `BaseMapper` |
| `AbstractBaseServiceImpl<T, ID, REPOSITORY>` | **服务基类**：实现 `BaseService`，内置钩子 `beforeSave`/`afterSave`/`beforeUpdate(old, cur)`/`afterUpdate` |
| `PermissionSqlInjector` | **数据权限注入器接口**：实现 `injectSql()` / `additionalParameters()` / `type()` 三个方法 |

## 注解（业务方使用）

| 注解 | 说明 |
|------|------|
| `@DataPermission(injectors = {...})` | 方法/类级数据权限，指定生效的 `PermissionSqlInjector`；`statementIds` 默认 `"ALL"` 作用于全部 SQL 语句；不加注解的方法不追加任何条件 |
| `@EnableDataPermission(basePackages = "...")` | 开启数据权限，扫描指定包下的注入器实现（无需 `@Component`） |
| `@TableAlias` | 查询别名辅助 |
| `@DataChangeLog`（继承自 repository） | 标记需要记录变更的实体 |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `beforeSave` / `afterSave` / `beforeUpdate` / `afterUpdate` | 服务基类生命周期钩子 |
| `PermissionSqlInjector.type()` | `InjectType.WHERE_ADDITIONAL` 追加 WHERE 条件；`InjectType.REPLACE_TABLE_NAME` 按格式 `A->B` 替换表名 |
| `DataPermissionPlugin`（MyBatis `Interceptor`） | 由 `@EnableDataPermission` 自动注册，与 `DataPermissionAspect`（AOP 织入上下文）配合：按 `statementIds` 匹配 MappedStatement，经 JSqlParser 改写 SELECT/UPDATE/DELETE 追加数据权限条件 |
| `DataChangeLogEventListener` Bean | 重写 `changelog(DataChangeLogDTO)` 自定义变更落地 |
| `InjectType` / `DataPermissionContextHolder` | 数据权限上下文与注入类型扩展 |
| `DataPermissionSupport` | 注入器获取与 `injectSql` 解析的公共实现（与 JPA 侧共用）：注入器未注册为 Bean 时退化为反射实例化并 `warn`，避免以 NPE / `NoSuchBeanDefinitionException` 形式暴露 |

## 使用示例

```java
// 1) Mapper 继承 MyBatis-Plus 基类
@Mapper
public interface EmployeeMapper extends BaseRepository<EmployeeEntity, Long> {}

// 2) 实体继承审计基类
@Data
@TableName("EMPLOYEE")
public class EmployeeEntity extends AbstractAuditEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Integer age;
}

// 3) 数据权限
// 3.1) 实现注入器（放在 @EnableDataPermission 扫描包下，无需 @Component）
public class DeptSqlInject implements PermissionSqlInjector {
    public String injectSql() { return "DEPT_ID = :deptId"; }
    public Map<String, Object> additionalParameters() { return Map.of("deptId", currentDeptId()); }
    public InjectType type() { return InjectType.WHERE_ADDITIONAL; }
}
// 3.2) 开启并使用
@Configuration
@EnableDataPermission(basePackages = "com.example.permission")
public class DataSourceConfiguration { ... }

@Service
public class EmployeeService extends AbstractBaseServiceImpl<EmployeeEntity, Long, EmployeeMapper> {
    @DataPermission(injectors = {DeptSqlInject.class})
    public List<EmployeeEntity> listByDept() { return this.findAll(); }
}

// 4) 前端提交的查询 JSON
// { "filter": "[[name, CONTAINS, 张], and, [age, >=, 18]]",
//   "sorts": "[{\"columns\":[\"createTime\"],\"direction\":\"DESC\"}]",
//   "page": 1, "limit": 20 }
```

> **包路径说明（3.x 起）**：业务方直接使用的基类（`BaseRepository`、`AbstractBaseServiceImpl`、`AbstractAuditEntity`）
> 位于 `org.wymix.framework.repository.mybatis.*` 子包，与 JPA 实现完全隔离，两模块可并存引入；
> 框架内部的配置与插件类则分布在 `...configuration`、`...datapermission`、`...datachangelog` 等各自子包，无需业务方直接引用。旧包路径 `org.wymix.framework.repository.api` /
> `...audit.domain` 的兼容别名已移除，请直接使用下列 import：
>
> ```java
> import org.wymix.framework.repository.mybatis.api.BaseRepository;
> import org.wymix.framework.repository.mybatis.api.AbstractBaseServiceImpl;
> import org.wymix.framework.repository.mybatis.audit.domain.AbstractAuditEntity;
> ```
