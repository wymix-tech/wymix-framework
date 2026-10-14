# README 一致性核对 · 13 · dynamic_datasource

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。
> 前置：请先完成 06 `repository`，本模块依赖它。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、`EnvironmentPostProcessor` 生命周期、Spring JDBC 与多数据源路由。

## 目标
核对 `dynamic_datasource` 的 README 中关于「依赖引入、配置前缀、自动装配、切换方式」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/dynamic_datasource`
- 待核对/修订文档：`/workspace/dynamic_datasource/README.md`
- 处理序号：13 / 16
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`org.wymix.framework:dynamic_datasource:${revision}`；父 pom `dev_parent:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：在应用启动前（`EnvironmentPostProcessor` 阶段）注入多数据源，支持运行时按需切换
- 内部依赖：`repository`
- 外部依赖：`spring-jdbc`
- 自动装配注册：`AutoConfiguration.imports` → `org.wymix.framework.dynamic.datasource.configuration.DynamicDatasourceConfiguration`
- `spring.factories`：`org.springframework.boot.env.EnvironmentPostProcessor` → `org.wymix.framework.dynamic.datasource.support.DynamicEnvPostProcessor`（文件注释明确：`EnvironmentPostProcessor` 必须先于自动装配阶段执行，只能在 `spring.factories` 注册，不可迁移到 `imports`）
- README 声明的配置前缀：`dynamic.datasource`
- 源码包根：`org.wymix.framework.dynamic.datasource`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致
3. README 是否说明需自行引入具体数据库的 JDBC 驱动（依赖中只有 `spring-jdbc`）
4. 「引入即生效」的表述需与「默认关闭（`enabled` 默认 `false`）+ 双注册机制（`EnvironmentPostProcessor` 走 `spring.factories`，装配走 `imports`）」一致

### B 自动装配与生效条件
5. README 装配类清单完整：`DynamicDatasourceConfiguration`、`DynamicEnvPostProcessor` 全限定名正确，且无遗漏的装配/支撑类
6. `dynamic.datasource.enabled`（README 称默认 `false`）是否真实存在；核对 `@ConditionalOnProperty` 的 `havingValue` 与 `matchIfMissing`；**区分「EnvironmentPostProcessor 注入配置」与「自动装配创建 Bean」两个阶段的各自开关**
7. Properties 类的 `prefix`、字段名、类型、默认值与 README 配置表逐条一致
8. 配置项完整性：漏列 / 多余 / 改名 / 默认值不符；特别注意 `items.<name>` 的 Map 嵌套绑定字段集合（README 列了 url/username/password/driverClassName，需核对是否还有连接池、类型、校验查询等字段）、`primary` 是否必填、无 `primary` 时的默认行为

### C 使用方式
9. **切换 API 必须与源码一致**：核对注解切换与编程式切换的实际类名、全限定名、方法名与参数（如注解名、`ContextHolder` 工具类方法名等），README 若写了不存在的方法名必须纠正
10. 若 README 要求继承某基类或实现某接口（如 `AbstractRoutingDataSource` 子类），核对签名与泛型参数
11. 若存在自定义注解（切换数据源注解），核对属性名（如 `value`/`name`）、默认值、可标注位置（方法/类）与优先级规则
12. README 提到的扩展点（自定义路由策略、自定义数据源创建器）是否有接口/SPI 或 `@ConditionalOnMissingBean` 依据
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. yaml 片段中 `dynamic.datasource.items.<name>` 的嵌套缩进是否真实可绑定

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 标记为「已废弃」的切换方式与代码现状一致
18. 「在 Environment 准备阶段注入」「支持运行时切换」等断言是否有代码依据；无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **`EnvironmentPostProcessor` 注册方式**：`spring.factories` 注释明确这是 Spring Boot 3 官方约定的唯一注册方式，**不可**在 README 中描述为「应迁移到 `AutoConfiguration.imports` 的遗留配置」。
- **两阶段职责是否描述清楚**：`DynamicEnvPostProcessor` 在环境阶段做什么（把配置写入 Environment？重排 PropertySource？），`DynamicDatasourceConfiguration` 随后构建什么（多个 `DataSource` Bean？路由数据源？`@Primary` 如何指定），README 不得混淆。
- **`enabled` 默认值 false**：业务方不显式开启则完全不生效，README 的「依赖引入」章节必须给出最小可用配置，否则无法上手。
- **`primary` 缺省行为**：未指定主数据源时，是按第一个 `items` key、还是报错、还是有默认名，README 表格写"—"应核对并补全。
- **注解切换的作用范围与清理**：核对注解是在方法级还是类级生效、嵌套调用是否传播、`ThreadLocal` 是否在 finally 清理（README 若声称"自动清理"必须有代码依据）。
- **事务与数据源切换**：README 若声称"支持事务"或"事务内可切换"，需核对是否存在限制（通常事务内无法切换）；无依据则改为中性描述。

## 执行流程
1. 完整遍历 `/workspace/dynamic_datasource/src/main`（全部 `.java` 与 `resources`，重点 `META-INF/spring.factories`、`AutoConfiguration.imports`），建立「代码事实清单」：装配类与 `@ConditionalOn*`、`EnvironmentPostProcessor` 实现与注入逻辑、Properties 绑定结构与字段初始值、DataSource 构建方式、切换注解与上下文工具类签名、`ThreadLocal` 清理位置。
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
- **「存疑项」清单**：代码无法明确判断之处（尤其事务边界内的切换语义），说明为何不臆断。
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
