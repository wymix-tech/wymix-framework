# README 一致性核对 · 06 · repository

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、ORM 抽象层设计、注解与查询 DSL 解析。

## 目标
核对 `repository` 的 README 中关于「依赖引入、公共契约注解、查询 DSL、装配情况」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/repository`
- 待核对/修订文档：`/workspace/repository/README.md`
- 处理序号：06 / 16（JPA / MyBatis 两套实现的公共抽象层）
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`org.wymix.framework:repository:${revision}`；父 pom `dev_parent:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：跨 ORM 的数据访问公共抽象——`@DataChangeLog`（变更审计）、`@DataMigration`（迁移触发）、查询 DSL 符号常量 `FilterSymbolConst` 与 filter 解析器（AST）
- 内部依赖：`core`
- 自动装配：README 声称「本身不包含自动配置」；`src/main/resources/META-INF` 下未发现注册文件
- README 声明的配置项：`data.migration.*`（`DataMigrationProperties`，README 称为"空容器/扩展占位"）、`data.changelog.basePackages`、`data-changelog.enabled`
- 源码包根：`org.wymix.framework.repository`
- 注意：`jpa_repository` 的自动装配类 `org.wymix.framework.repository.configuration.JpaConfiguration` 使用了同名包前缀，核对时须确认其**实际所属模块**（跨模块归属不明时列入存疑项）

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致
3. README「通常不需要直接依赖，引入 `jpa_repository` 或 `mybatis_repository` 时会自动传递依赖」是否正确（本模块是否被两模块以 compile 依赖）
4. 「本身不包含自动配置」表述成立：确认无 `AutoConfiguration.imports`、`spring.factories`

### B 自动装配与生效条件
5. 源码中若实际存在 `@Configuration` / `@AutoConfiguration` 类，README「无自动配置」需按代码修正
6. 核对是否存在 `@ConditionalOn*`；README 声称配置项「由下层 ORM 模块读取」，需确认本模块自身是否真的不读取
7. `DataMigrationProperties`（前缀 `data.migration`）的 `prefix`、字段是否与 README 一致；README 称其为"空容器"，需核对是否真的没有任何字段
8. 配置项归属描述：README 说 `data-changelog.enabled`（默认 `true`）与 `data.changelog.basePackages`「由下层 ORM 模块装配」，若本模块自身已有绑定类或使用处，需按代码修正归属说明

### C 使用方式
9. `@DataChangeLog` / `@DataMigration` 注解的全限定名、属性名、默认值、可标注位置是否与源码一致
10. `FilterSymbolConst` 常量清单与 README 列出的符号（操作符）是否一致：漏列 / 多余 / 字符串取值不符
11. filter 解析器（AST）相关类：类名、全限定名、解析入口方法签名是否与 README 一致
12. README 提到的扩展点（如自定义解析、自定义符号）是否有接口/SPI 依据
13. README 的重要提示：`DataChangeLog` 注解在多个模块中各自定义、包路径不同——需核对现状是否仍然如此，避免出现已不存在的包路径
14. 示例代码能否编译、import 路径是否真实存在
15. filter DSL 示例字符串是否与解析器实际语法一致（嵌套括号、分隔符、逻辑关键字）

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 标记为「已废弃」的类/包与代码现状一致
18. 「共享抽象层」「为空占位」等断言是否有代码依据；无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **`DataChangeLog` 重复定义警告**：README 的注意事项涉及其它模块的同名注解，务必核对当前仓库中同名类的实际包路径（用 `org.wymix.framework.repository` 下的 import 是否能解析），若已合并则删除该警告。
- **`DataMigrationProperties` 是否真空**：README 称「当前为空容器，作为扩展占位」，核对字段数量是否为 0；若有字段必须补全配置表。
- **`FilterSymbolConst` 符号与字符串值**：配置/查询里写的是字符串（如 `>=`、`CONTAINS`），README 表格的取值必须与常量值完全一致，含大小写。
- **filter DSL 语法**：README 示例形如 `[[name, CONTAINS, 张], AND, [age, >=, 18]]`，需与解析器支持的 token、分隔符、嵌套层数限制逐一核对。
- **是否真的无自动配置**：留意是否存在 `@AutoConfiguration` 但未被注册到 `imports` 文件（这种"半成品"要在 README 中如实说明），或存在 `@Component`/`@Configuration` 被组件扫描命中的类。
- **配置项跨模块归属**：本模块 README 描述的配置项实际可能由 jpa/mybatis 模块读取，核对时如无法在本模块确认，`结论` 列写「存疑」，不要跨模块下结论。

## 执行流程
1. 完整遍历 `/workspace/repository/src/main`（全部 `.java` 与 `resources`，含 `META-INF`），建立「代码事实清单」：注解定义（属性与默认值）、常量类（常量名与取值）、解析器入口与支持的语法、Properties 类、是否存在装配类。
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
- **「存疑项」清单**：代码无法明确判断之处（尤其是跨模块的配置归属与同名注解），说明为何不臆断。
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
