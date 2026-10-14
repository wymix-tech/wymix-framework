# README 一致性核对 · 14 · data_migration

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。
> 前置：请先完成 07 `jpa_repository` 与 08 `mybatis_repository`，本模块同时依赖二者。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、Spring 事件机制与 ORM 变更日志/迁移机制。

## 目标
核对 `data_migration` 的 README 中关于「依赖引入、配置项、自动装配、事件触发链路」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/data_migration`
- 待核对/修订文档：`/workspace/data_migration/README.md`
- 处理序号：14 / 16（依赖两套 ORM 实现，最易出现重复 Bean 与描述漂移）
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`org.wymix.framework:data_migration:${revision}`；父 pom `dev_parent:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：监听数据变更事件并触发数据迁移，作为数据同步/迁移的扩展骨架
- 内部依赖：`jpa_repository`、`mybatis_repository`（**同时依赖两套 ORM**）
- 自动装配注册：`AutoConfiguration.imports` → `org.wymix.framework.data.migration.configuration.DataMigrationConfiguration`
- README 声明的配置项：`wymix.data-migration.enabled`（默认 `true`）、`data.migration.*`（`DataMigrationProperties`，README 称为空容器/扩展占位）
- README 声称的链路：`@DataChangeLog` 数据变更 → ORM 模块发出 `DataChangeLogEvent` → `DataMigrationEventListener` 监听 → 检查实体是否标注 `@DataMigration` → 命中则触发迁移回调
- 源码包根：`org.wymix.framework.data.migration`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致
3. **README「需与 `jpa_repository` 或 `mybatis_repository` 配合使用（事件来源）」是否准确**：本模块同时 compile 依赖二者，README 应如实说明「引入本模块会同时带入两套 ORM」，避免业务方误解为二选一
4. 「引入即生效」的表述与 `AutoConfiguration.imports` 注册方式一致；确认无 `spring.factories`

### B 自动装配与生效条件
5. README 装配类清单完整：`DataMigrationConfiguration` 全限定名正确，且无遗漏的装配类
6. `wymix.data-migration.enabled`（README 称默认 `true`）是否真实存在；核对 `@ConditionalOnProperty` 的 `havingValue` 与 `matchIfMissing`；是否存在其它条件（如 `@ConditionalOnBean` 依赖 ORM 模块的 Bean）
7. `DataMigrationProperties`（前缀 `data.migration`）的 `prefix`、字段是否与 README 一致；README 称其为「当前为空容器」，需核对字段数量是否真为 0
8. 配置项完整性：漏列 / 多余 / 改名 / 默认值不符 / 嵌套结构不一致

### C 使用方式
9. `DataMigrationEventListener`、`DataMigration` 注解、`DataChangeLogEvent`（或等价事件）的类名、全限定名、方法签名是否与源码一致
10. 若 README 要求继承某基类或实现某接口（如自定义迁移处理器），核对签名与泛型参数
11. `@DataMigration` 注解的属性名、默认值、可标注位置是否与源码一致（注意该注解定义在 `repository` 模块还是本模块，README 引用的包路径必须能解析）
12. 「声明自定义 Bean 即覆盖默认迁移实现」是否有 `@ConditionalOnMissingBean` 支撑；迁移回调的接口名是否与源码一致
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. 若有 yaml 片段，缩进与前缀是否真实可绑定

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 标记为「已废弃」的内容与代码现状一致
18. **「监听事件并触发迁移」「作为数据同步/迁移的扩展骨架」等断言必须有明确代码依据**：核对真实存在的监听方法、事件类型与回调入口；若无实际迁移能力（仅骨架），README 措辞需与实现程度一致

## 本模块重点核对项（易错点）
- **事件链路是否真实连通**：README 描述的 `DataChangeLogEvent` → `DataMigrationEventListener` 链路，需核对事件类的实际包路径与监听方法签名（`@EventListener` 还是实现 `ApplicationListener`，同步还是 `@Async`）。事件类定义在 ORM 模块时，本 README 只描述本模块侧行为，跨模块归属列入存疑项。
- **同时依赖两套 ORM 的后果**：是否导致两套 ORM 的自动装配都被激活、是否存在 Bean 冲突或条件保护；README「需与其中之一配合」的说法与 pom 事实矛盾，需按代码修正。
- **`DataMigrationProperties` 是否真空**：核对字段数量是否为 0，若为 0 保留「扩展占位」表述；若有字段必须补全配置表。
- **`wymix.data-migration.enabled` 命名**：含连字符，与 `data.migration` 前缀并存，务必按源码逐字记录，勿改写。
- **迁移回调的触发条件**：核对「实体标注 `@DataMigration` 才触发」的判断是在事件监听器内还是另有过滤器；是否有开关控制。
- **仅骨架还是具备实现**：若模块只有事件监听占位而无实际迁移逻辑，README 的「数据同步/迁移」表述应相应弱化。

## 执行流程
1. 完整遍历 `/workspace/data_migration/src/main`（全部 `.java` 与 `resources`，含 `META-INF`），建立「代码事实清单」：装配类与 `@ConditionalOn*`、Properties 绑定结构与字段初始值、事件监听方法签名与监听的事件类型、注解引用（含其定义的包路径能否解析）、回调接口。
2. 通读 README，逐节比对。
3. 输出 18 项核对结论表。
4. 对每条差异给出处理结论：`修正 README` / `删除过时描述` / `存疑待人工确认`。
5. 仅修改 README，最小必要改动。

## 输出要求（先清单，再改文档）
先输出：

| # | 核对项 | README 原文位置 | README 描述 | 代码实际 | 结论 |
|---|--------|-----------------|-------------|----------|------|

修改完成后输出：
- **「已修改」清单**：逐条说明改了什么、依据哪个源文件的哪段代码（给出文件路径与关键行）。
- **「存疑项」清单**：代码无法明确判断之处（尤其事件类归属 `jpa` / `mybatis` / `repository` 模块的哪一个），说明为何不臆断。
- 若 18 项全部一致，明确输出「本模块 18 项均一致，README 无需修改」，并**不作任何改写**。

修订 README 时遵守：
- 保留原有章节结构、标题层级、表格风格与语气，不推倒重写、不扩充无关内容。
- 配置表维持「配置项 | 说明 | 默认值」三列格式。
- 不新增未经代码验证的能力描述；不为凑完整性编造默认值或用法。
- 概述性表述无明确错误的，不要改动。

## 硬性约束
- 事实基准只有源码；源码与 README 冲突即以源码为准。
- 不得修改任何 `.java` / `.xml` / `.yml` 文件，本次产物只有 README。
- 不得删除 README 中代码仍然存在的能力描述。
- 不输出「已全部核对完成」之类的结论性套话，用具体清单证明核对过程。
