# travelsky_dev_data_migration 数据迁移模块

> 监听数据变更事件并触发数据迁移，作为数据同步/迁移的扩展骨架。

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

> 需与 `jpa_repository` 或 `mybatis_repository` 配合使用（事件来源）。

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `travelsky.data-migration.enabled` | 是否启用数据迁移模块 | `true` |
| `data.migration.*` | 迁移行为属性（`DataMigrationProperties`，当前为空容器，扩展占位） | — |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `DataMigrationConfiguration` | 自动装配入口，同时 `@Import` 变更日志配置 |
| `DataMigrationEventListener` | 监听 `DataChangeLogEvent`，对标注 `@DataMigration` 的实体触发迁移 |

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
