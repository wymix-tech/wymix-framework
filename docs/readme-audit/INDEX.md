# README 一致性核对提示词索引（单模块逐个处理）

本目录为**每个模块生成一份自包含的专有提示词**，用于核对模块 README 与实际代码的一致性。
规则：**不一致一律以当前代码为准**，产物只有 README，不改动任何源码。

## 使用方式

1. 按下方顺序逐个处理（已完成在前模块 README 的基础上继续，避免重复返工）。
2. 打开对应模块的提示词文件，**整段复制**给 AI（AI 需以本仓库为工作区）。
3. 每完成一个模块单独提交一次改动，便于评审与回滚。
4. 处理完在下表勾选。

## 依赖关系（依据各模块 pom.xml 的内部依赖推导）

```
L0  utils   threadpool   test   ai          （无内部依赖）
L1  common ──→ utils        cache ──→ utils
L2  core ──→ common, threadpool
L3  repository ──→ core
L4  jpa_repository ──→ repository        mybatis_repository ──→ repository
    security ──→ core                    monitor ──→ core
    decentralization_discovery ──→ core
L5  web ──→ core, security               dynamic_datasource ──→ repository
L6  data_migration ──→ jpa_repository, mybatis_repository
```

> Maven 的 `<modules>` 声明顺序与依赖顺序无关，本表按真实依赖分层排序。
> 同层内的顺序可按业务重要性调整，`test` 与 `ai` 无内部依赖，放在最后统一处理。

## 处理顺序清单

| 序号 | 模块 | 内部依赖 | 提示词文件 | 完成 |
|------|------|----------|-----------|------|
| 01 | `utils` | — | [01-utils.md](./01-utils.md) | ☑ |
| 02 | `threadpool` | — | [02-threadpool.md](./02-threadpool.md) | ☑ |
| 03 | `common` | utils | [03-common.md](./03-common.md) | ☑ |
| 04 | `cache` | utils | [04-cache.md](./04-cache.md) | ☑ |
| 05 | `core` | common, threadpool | [05-core.md](./05-core.md) | ☑ |
| 06 | `repository` | core | [06-repository.md](./06-repository.md) | ☑ |
| 07 | `jpa_repository` | repository | [07-jpa_repository.md](./07-jpa_repository.md) | ☑ |
| 08 | `mybatis_repository` | repository | [08-mybatis_repository.md](./08-mybatis_repository.md) | ☑ |
| 09 | `security` | core | [09-security.md](./09-security.md) | ☑ |
| 10 | `monitor` | core | [10-monitor.md](./10-monitor.md) | ☑ |
| 11 | `decentralization_discovery` | core | [11-decentralization_discovery.md](./11-decentralization_discovery.md) | ☑ |
| 12 | `web` | core, security | [12-web.md](./12-web.md) | ☑ |
| 13 | `dynamic_datasource` | repository | [13-dynamic_datasource.md](./13-dynamic_datasource.md) | ☑ |
| 14 | `data_migration` | jpa_repository, mybatis_repository | [14-data_migration.md](./14-data_migration.md) | ☑ |
| 15 | `test` | — | [15-test.md](./15-test.md) | ☑ |
| 16 | `ai` | — | [16-ai.md](./16-ai.md) | ☑ |

## 全仓库通用基线（各模块提示词已内置，供人工抽查）

| 项 | 实际值 |
|----|--------|
| groupId | `org.wymix.framework` |
| 父 pom | `dev_parent`，version `${revision}` = `3.0.0-SNAPSHOT` |
| Java | 17 |
| Spring Boot | 3.5.8 |
| 自动装配注册 | Spring Boot 3 规范：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`；`EnvironmentPostProcessor` / `ApplicationListener` 仍走 `META-INF/spring.factories` |

## 自动装配注册现状（用于定位，最终以源码为准）

| 模块 | `AutoConfiguration.imports` | `spring.factories` |
|------|------------------------------|--------------------|
| core | `CoreConfiguration`、`AsyncScheduleAutoConfiguration` | 2 个 `ApplicationListener` |
| common | 无 | 无 |
| utils | 无 | 无 |
| threadpool | `ThreadPoolConfiguration` | 无 |
| cache | `CacheManagerConfiguration` | 无 |
| repository | 无 | 无 |
| jpa_repository | `JpaConfiguration`、`DataChangeLogConfiguration` | 无 |
| mybatis_repository | `MyBatisConfiguration`、`DataChangeLogConfiguration` | 无 |
| security | `CustomSecurityConfiguration` | `DecryptEnvironmentPostProcessor` |
| monitor | `MonitorConfiguration` | `MonitorApplicationStartedEventListener` |
| decentralization_discovery | `DecentralizedDiscoveryConfiguration` | 无 |
| web | `WebConfiguration`、`ApiDocAutoConfiguration` | 无 |
| dynamic_datasource | `DynamicDatasourceConfiguration` | `DynamicEnvPostProcessor` |
| data_migration | `DataMigrationConfiguration` | 无 |
| test | 无 | 无 |
| ai | 无 | 无 |

> 通用模板（不含模块专有上下文）见 `../README_AUDIT_PROMPT.md`。
