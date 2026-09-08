# README 一致性核对 · 07 · travelsky_dev_jpa_repository

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、Spring Data JPA、Hibernate 事件机制与 Specification 查询。

## 目标
核对 `travelsky_dev_jpa_repository` 的 README 中关于「依赖引入、配置项、自动装配、实体/仓储/服务基类、查询 DSL」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/travelsky_dev_jpa_repository`
- 待核对/修订文档：`/workspace/travelsky_dev_jpa_repository/README.md`
- 处理序号：07 / 16
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`com.travelsky.base:travelsky_dev_jpa_repository:${revision}`；父 pom `travelsky_dev_parent3:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：基于 Spring Data JPA 的持久化实现——审计字段自动填充、数据变更日志、统一查询 DSL
- 内部依赖：`travelsky_dev_repository`
- 外部依赖：`spring-boot-starter-data-jpa`、`jsqlparser 5.2`、`spring-boot-starter-web`（compile）、test 用 junit
- 自动装配注册：`AutoConfiguration.imports` → `com.travelsky.base.repository.configuration.JpaConfiguration`、`com.travelsky.base.repository.jpa.datachangelog.configuration.DataChangeLogConfiguration`
- README 声明的配置项：`travelsky.jpa.enabled`（默认 `true`）、`data-changelog.enabled`（默认 `true`）、`data.changelog.basePackages`、`spring.jpa.*`
- 源码包根：`com.travelsky.base.repository.jpa`（README 明确 3.x 起 JPA 专属类统一在该子包，与 MyBatis 实现隔离）
- README 声明旧兼容包路径已移除：`com.travelsky.base.repository.api` / `...audit.domain`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致
3. README 是否说明与 `mybatis_repository` 可并存引入；`travelsky_dev_repository` 是否传递引入
4. 「引入即自动生效」与 `AutoConfiguration.imports` 注册方式一致；确认无 `spring.factories`

### B 自动装配与生效条件
5. README 装配类清单完整：`JpaConfiguration`、`DataChangeLogConfiguration` 全限定名正确，且无遗漏的装配类
6. `travelsky.jpa.enabled`、`data-changelog.enabled` 是否真实存在；核对 `@ConditionalOnProperty` 的 `havingValue` 与 `matchIfMissing`，确认默认是否真为 `true`
7. Properties 类的 `prefix`、字段名、类型、默认值与 README 配置表逐条一致（注意 `data-changelog.enabled` 连字符风格与 `data.changelog.basePackages` 点分风格并存，务必以源码为准）
8. 配置项完整性：漏列 / 多余 / 改名 / 默认值不符 / 嵌套结构不一致

### C 使用方式
9. `AbstractAuditEntity`、`BaseRepository<T, ID>`、`AbstractBaseServiceImpl<T, ID, REPOSITORY>` 的全限定名、泛型参数个数、构造器签名是否与源码一致
10. `AbstractBaseServiceImpl` 实现的 `BaseService` 接口及其方法清单；钩子方法 `beforeSave` / `afterSave` / `beforeUpdate(old, cur)` / `afterUpdate` 的签名与可见性（`protected`）是否准确
11. 若存在自定义注解，核对属性名、默认值、可标注位置
12. 「声明自定义 `DataChangeLogEventListener` Bean 即覆盖默认实现」是否有 `@ConditionalOnMissingBean` 支撑；`ManualDmlContext` 手动触发方式是否与源码一致
13. **README 末尾的包路径说明**：所列三条 import（`com.travelsky.base.repository.jpa.api.BaseRepository`、`...jpa.api.AbstractBaseServiceImpl`、`...jpa.audit.domain.AbstractAuditEntity`）必须逐一确认真实存在；「旧兼容别名已移除」需核实
14. 示例代码能否编译（含 Lombok `@Data`、`@Entity` 用法）、import 路径是否真实存在
15. 若有 yaml 片段，缩进与前缀是否真实可绑定

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 标记为「已废弃/计划移除」的内容（旧包路径兼容别名）与代码现状一致
18. 「与 MyBatis 实现完全隔离」「两模块可并存引入」等断言是否有代码依据；无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **包路径迁移结论必须实证**：README 称旧包 `...repository.api` / `...audit.domain` 兼容别名已移除——请搜索全仓库确认，若旧类仍存在则 README 表述需修正。
- **变更日志装配细节**：`DataChangeLogConfiguration` 声称挂载 Hibernate 事件监听器、请求拦截器、`ManualDmlAspect`、`BatchDeleteAspect`，需核对这些 Bean/切面是否真实存在、切面表达式与 README 描述一致。
- **`data-changelog.enabled` 与 `data.changelog.basePackages` 命名不一致**（连字符 vs 点分）：分别核对实际绑定源码，不要统一改写，README 必须如实呈现。
- **`SpecificationHelper` / `PageHelper`** 的方法签名与 `DevQueryVO` 的 `filter` / `sorts` 字段格式：README 示例 `q.setSorts("[{\"columns\":[...],\"direction\":\"DESC\"}]")` 需与解析器期望结构一致。
- **审计字段填充机制**：`DatabaseAuditorAware` / `AuditHandler` / `AuditListener` 三者关系与字段名 `createdBy` / `createdTime` / `lastModifiedBy` / `lastModifiedTime` 是否与源码一致。
- **与 mybatis 模块并存的 Bean 冲突**：同名 Bean 是否有 `@ConditionalOnMissingBean` / `@ConditionalOnClass` 保护（无法在本模块确认时列入存疑项）。

## 执行流程
1. 完整遍历 `/workspace/travelsky_dev_jpa_repository/src/main`（全部 `.java` 与 `resources`，含 `META-INF`），建立「代码事实清单」：装配类与 `@ConditionalOn*`、Properties 绑定结构与字段初始值、基类与接口的签名/泛型、钩子方法、切面与监听器、事件 DTO。
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
- **「存疑项」清单**：代码无法明确判断之处（尤其并存冲突、跨模块注解来源），说明为何不臆断。
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
