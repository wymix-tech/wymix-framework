# travelsky_dev_repository 数据访问抽象层

> 定义跨 ORM 的数据访问公共抽象：变更日志、数据迁移与统一查询符号常量。

## 模块说明

`travelsky_dev_repository` 是 JPA 与 MyBatis-Plus 两套实现共享的抽象层，定义与具体持久化技术无关的公共契约：`DataChangeLog`（数据变更审计）、`DataMigration`（数据迁移触发）、以及查询 DSL 的符号常量 `FilterSymbolConst`。它本身不包含自动配置，而是为下层 `jpa_repository` / `mybatis_repository` 提供基类与约定。

> ⚠️ 注意：`DataChangeLog` 注解在 `repository`、`jpa_repository` 等多个模块中各自定义，包路径不同。请统一引用本模块（或 `jpa_repository`）中的定义，避免重复引用造成歧义。

## 功能说明

- **数据变更日志** `@DataChangeLog`：标记需要记录增删改的实体/方法，由 `jpa_repository` 的拦截器与 AOP 落地（MyBatis 侧另有实现）。
- **数据迁移** `@DataMigration` / `DataMigrationProperties`：标记数据迁移任务，框架在检测到变更事件后可触发迁移（详见 `data_migration` 模块）。
- **查询符号常量** `FilterSymbolConst`：集中定义 filter DSL 的操作符与连接符字符串，供解析器与前端共用。

## 配置项

| 配置前缀 | 说明 |
|----------|------|
| `data.migration.*` | 数据迁移相关属性（`DataMigrationProperties`，当前为空容器，作为扩展占位） |

## 使用示例

```java
// 1) 标记实体需要记录变更日志
@DataChangeLog("EMPLOYEE")
@Entity
public class Employee { ... }

// 2) 在方法上标记需要迁移的数据
@DataMigration(table = "EMPLOYEE")
public void syncEmployee(Employee e) { ... }

// 3) 引用统一查询符号（避免硬编码字符串）
String op = FilterSymbolConst.CONTAINS; // "CONTAINS"
```

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_repository</artifactId>
    <version>${revision}</version>
</dependency>
```

> 通常不需要直接依赖本模块，引入 `jpa_repository` 或 `mybatis_repository` 时会自动传递依赖。
