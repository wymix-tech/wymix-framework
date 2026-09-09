# travelsky_dev_data_migration 数据迁移模块

> 监听数据变更事件并检查实体是否标注 `@DataMigration`，作为数据同步/迁移的扩展骨架（内置迁移回放尚未实现）。

## 模块说明

`travelsky_dev_data_migration` 与 `repository` 模块的 `@DataMigration` 注解配合：当标注了 `@DataChangeLog` 的数据发生变更（由 ORM 模块的变更日志机制发出 `DataChangeLogEvent` 事件）时，`DataMigrationEventListener` 监听事件并检查对应实体是否标注 `@DataMigration`，命中则触发迁移回调。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_data_migration</artifactId>
    <version>${revision}</version>
</dependency>
```

> 需与 `jpa_repository` / `mybatis_repository` 配合使用（事件来源）。引入本模块会通过 compile 依赖同时带入两套 ORM 模块；本模块装配类会按 classpath 条件自动导入对应的 `DataChangeLogConfiguration`，而非二选一。

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `travelsky.data-migration.enabled` | 是否启用数据迁移模块 | `true` |
| `data.migration.*` | 迁移行为属性（`DataMigrationProperties`，当前为空容器，扩展占位）。注意：该 Properties **当前未通过 `@EnableConfigurationProperties` / `@ConfigurationPropertiesScan` 注册，配置不会被绑定**，写入 `data.migration.*` 不会生效 | — |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `DataMigrationConfiguration` | 自动装配入口，同时 `@Import` 变更日志配置 |
| `DataMigrationEventListener` | 实现 `ApplicationListener` 监听 `DataChangeLogEvent`，对标注 `@DataMigration` 的实体触发迁移（当前迁移回放未实现，仅 debug 日志）。事件 DTO 通过 `getDataChangeLogDTO()` 获取；注意 `DataChangeLogEvent` 存在不携带 DTO 的构造（如 `(Object, Clock)`），该场景下取值为 `null` |

## 继承类 / 注解（业务方使用）

| 类/注解 | 说明 |
|---------|------|
| `@DataMigration` | 类级注解，标注在实体上（见 `repository` 模块） |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `@EventListener(DataChangeLogEvent)` | 业务方可自行监听变更事件，实现自定义迁移逻辑 |

## 使用示例

```java
// 1) 标注实体（发生增删改时模块会收到事件）
// 说明：以下为 MyBatis 场景示例，@TableName 为 mybatis-plus 注解、
// AbstractAuditEntity 来自 travelsky_dev_mybatis_repository；
// JPA 场景请改用 jpa_repository 中的同名基类（两者包路径不同，不可混用）
@DataMigration
@TableName("EMPLOYEE")
public class EmployeeEntity extends AbstractAuditEntity { ... }

// 2) 自定义迁移逻辑（当前模块为骨架，迁移动作需业务实现）
@Component
public class MyMigrationListener {

    @EventListener
    public void onChange(DataChangeLogEvent event) {
        DataChangeLogDTO dto = event.getDataChangeLogDTO();
        // 同步到 ES / 写入审计库 / 推送下游 ...
    }
}
```

> ⚠️ 当前该模块为骨架实现：事件监听已挂载，但内置的「变更日志 → 迁移脚本」回放尚未实现（仅 debug 日志），迁移动作需业务方通过事件监听自行落地。
