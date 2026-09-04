# travelsky_dev_data_migration 数据迁移模块

> 监听数据变更事件并触发数据迁移，作为数据同步/迁移的扩展骨架。

## 模块说明

`travelsky_dev_data_migration` 与 `repository` 模块的 `@DataMigration` 注解配合：当数据发生变更（由 `jpa_repository` 的变更日志机制发出事件）时，`DataMigrationEventListener` 监听事件并尝试触发对应的迁移逻辑。该模块提供「事件 → 迁移」的扩展点。

## 功能说明

- **事件监听** `DataMigrationEventListener`：监听框架发出的数据变更事件。
- **属性占位** `DataMigrationProperties`（前缀 `data.migration`）：当前为扩展占位容器，便于后续增加迁移行为配置。

## 配置项

配置前缀：`data.migration`

| 配置项 | 说明 |
|--------|------|
| `data.migration.*` | 当前为空容器，作为后续迁移行为配置的占位前缀 |

## 使用示例

```java
// 1) 标记需要迁移的数据（在 jpa_repository 侧定义 @DataMigration）
@DataMigration(table = "EMPLOYEE")
public void syncToSearchIndex(Employee e) { ... }

// 2) 自定义迁移监听器（监听框架事件后落地你的迁移逻辑）
@Component
public class MyMigrationListener {
    // 通过监听 DataMigrationEventListener 发布的事件实现自定义迁移
}
```

> ⚠️ 当前该模块为骨架实现：事件监听已挂载，但具体的迁移执行方法体为空，需要业务方自行实现迁移逻辑。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_data_migration</artifactId>
    <version>${revision}</version>
</dependency>
```
