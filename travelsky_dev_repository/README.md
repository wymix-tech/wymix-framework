# travelsky_dev_repository 数据访问抽象层

> 定义跨 ORM 的数据访问公共抽象：变更日志、数据迁移与统一查询 DSL。

## 模块说明

`travelsky_dev_repository` 是 JPA 与 MyBatis-Plus 两套实现共享的抽象层，定义与具体持久化技术无关的公共契约：`@DataChangeLog`（数据变更审计）、`@DataMigration`（数据迁移触发）、以及查询 DSL 的符号常量 `FilterSymbolConst` 与 filter 解析器（AST）。它本身不包含自动配置，而是为下层 `jpa_repository` / `mybatis_repository` 提供基类与约定。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_repository</artifactId>
    <version>${revision}</version>
</dependency>
```

> 通常不需要直接依赖本模块，引入 `jpa_repository` 或 `mybatis_repository` 时会自动传递依赖。

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `data-changelog.enabled` | 变更日志开关（由下层 ORM 模块装配，本模块不读取） | `true`（由 jpa/mybatis 模块 `@ConditionalOnProperty(matchIfMissing = true)` 决定） |
| `data.changelog.basePackages` | 变更日志扫描包（由下层 ORM 模块读取，逗号分隔） | 空 |
| `data.migration.*` | 数据迁移相关属性（`DataMigrationProperties` 定义于 `travelsky_dev_data_migration` 模块，当前为空容器，作为扩展占位）。注意：`data.migration.*` 前缀在本模块及依赖方均无读取方，数据迁移装配实际开关为 `travelsky.data-migration.enabled`（默认 `true`） | — |

## 集成类

本模块无自动配置，通过下层模块的自动装配生效（`DataChangeLogEvent` 事件、`DataChangeLogEventListener` 监听器均定义于此）。

## 注解清单（业务方使用）

| 注解 | 说明 |
|------|------|
| `@DataChangeLog` | 标记需要记录增删改的实体；`record()` 可指定记录的语句类型（INSERT/MODIFY/REMOVE） |
| `@DataMigration` | 类级注解，标记实体发生变更时触发数据迁移（配合 `data_migration` 模块） |

## 继承类（业务方可继承/实现）

| 类/接口 | 说明 |
|---------|------|
| `DataChangeLogEventListener` | 变更日志事件监听器，业务方可声明子类 Bean 并重写 `changelog(DataChangeLogDTO)` 自定义处理（默认仅记录日志） |
| `FilterParser` / `FilterStringParser` | filter DSL → AST 解析器，可直接使用或扩展 |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `DataChangeLogEvent` | Spring 事件，`@EventListener` 可监听数据变更 |
| `FilterSymbolConst` | 集中定义 filter DSL 的分隔符常量（方括号、逗号），操作符取值见 `com.travelsky.base.common.query.OperateSymbol` |
| `StatementType` | 变更语句类型枚举，扩展 `@DataChangeLog(record = ...)` 可选值 |

## 使用示例

```java
// 1) 标记实体需要记录变更日志
@DataChangeLog
@Entity
public class Employee { ... }

// 2) 标记实体需要触发数据迁移
@DataMigration
@TableName("EMPLOYEE")
public class EmployeeEntity { ... }

// 3) 自定义变更日志处理
@Bean
public DataChangeLogEventListener dataChangeLogEventListener() {
    return new DataChangeLogEventListener() {
        @Override
        protected void changelog(DataChangeLogDTO dto) {
            // 落库 / 推送 MQ / 写 ES ...
        }
    };
}

// 4) 操作符取值（本模块不提供，来自 common 模块，经 core 传递引入）
String op = OperateSymbol.CONTAINS.getSymbol()[0]; // "CONTAINS"

// 5) DSL 分隔符常量（避免硬编码）
String left = FilterSymbolConst.LEFT_BUCKET_STR;   // "["
String comma = FilterSymbolConst.COMMA_STR;        // ","
```
