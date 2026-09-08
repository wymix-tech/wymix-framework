# README 一致性核对 · 08 · travelsky_dev_mybatis_repository

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、MyBatis-Plus 3.5.x、SQL 解析与数据权限 AOP。

## 目标
核对 `travelsky_dev_mybatis_repository` 的 README 中关于「依赖引入、配置项、自动装配、分页插件、数据权限、查询 DSL 文档链接」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/travelsky_dev_mybatis_repository`
- 待核对/修订文档：`/workspace/travelsky_dev_mybatis_repository/README.md`
- 处理序号：08 / 16
- **关联文档（本模块 README 显式引用的外部文档，属于核对范围）**：
  - `/workspace/travelsky_dev_mybatis_repository/DevQueryVO用法.md`
  - `/workspace/FILTER_USAGE_GUIDE.md`
  若 README 描述的查询语法与上述文档/源码不一致，以源码为准并在「已修改」清单中说明；**只允许修改本模块 README**，不得改动上述两个被引用的文档。
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`com.travelsky.base:travelsky_dev_mybatis_repository:${revision}`；父 pom `travelsky_dev_parent3:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：基于 MyBatis-Plus 的持久化实现——分页插件、数据权限 AOP、统一查询 DSL（`DevQueryVO`）
- 内部依赖：`travelsky_dev_repository`
- 外部依赖：`mybatis-plus-spring-boot3-starter`、`mybatis-plus-jsqlparser`（BOM `mybatis-plus-bom 3.5.15`）、`spring-boot-starter-web`（compile）、junit(test)
- 自动装配注册：`AutoConfiguration.imports` → `com.travelsky.base.repository.configuration.MyBatisConfiguration`、`com.travelsky.base.repository.mybatis.datachangelog.configuration.DataChangeLogConfiguration`
- README 声明的配置项：`travelsky.mybatis.enabled`（默认 `true`）、`data-changelog.enabled`（默认 `true`）、`data.changelog.basePackages`、`mybatis-plus.*`
- 源码包根：`com.travelsky.base.repository.mybatis`
- 已知约束：pom 中 pitest 目标类为 `com.travelsky.base.repository.query.helper.QueryParseHelper*`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致；**MyBatis-Plus 版本（3.5.15）与 MyBatis-Plus Spring Boot 3 Starter 的对应版本是否被正确说明**
3. README 是否说明与 `jpa_repository` 可并存引入；`travelsky_dev_repository` 是否传递引入
4. 「引入即自动生效」与 `AutoConfiguration.imports` 注册方式一致；确认无 `spring.factories`

### B 自动装配与生效条件
5. README 装配类清单完整：`MyBatisConfiguration`、`DataChangeLogConfiguration` 全限定名正确，且无遗漏的装配类
6. `travelsky.mybatis.enabled`、`data-changelog.enabled` 是否真实存在；核对 `@ConditionalOnProperty` 的 `havingValue` 与 `matchIfMissing`，确认默认是否真为 `true`
7. Properties 类的 `prefix`、字段名、类型、默认值与 README 配置表逐条一致（注意 `data-changelog.enabled` 连字符风格与 `data.changelog.basePackages` 点分风格并存，以源码为准）
8. 配置项完整性：漏列 / 多余 / 改名 / 默认值不符 / 嵌套结构不一致；`mybatis-plus.*` 为第三方前缀，标注清楚不受本模块控制

### C 使用方式
9. README 示例中的查询 DSL 入口（`DevQueryVO`、`QueryParseHelper` 等）类名、全限定名、方法签名是否与源码一致
10. 若 README 要求继承某基类或实现某接口（如审计基类、Service 基类），核对签名与泛型参数
11. `@DataPermission` 注解的属性名、默认值、可标注位置是否与源码一致
12. README 提到的扩展点（自定义数据权限处理、自定义拦截器等）是否有接口/SPI 或 `@ConditionalOnMissingBean` 依据
13. README 提到的包路径迁移、旧别名是否仍成立（本模块与 JPA 实现是否已包名隔离）
14. 示例代码能否编译、import 路径是否真实存在
15. 若有 yaml 片段，缩进与前缀是否真实可绑定

### D 其他描述
16. **README 引用的两个文档链接必须验证存在性**：`./DevQueryVO用法.md` 与根目录 `FILTER_USAGE_GUIDE.md`
17. README 标记为「已废弃」的内容与代码现状一致
18. 「分页插件 `maxLimit=10000`」「审计字段自动填充」「类 Lisp 嵌套括号表达式」等断言是否有代码依据；无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **分页插件 `maxLimit` 实际值**：README 写 `10000`，需核对拦截器中设置的数值，以及超出限制时的行为（截断/抛异常）。
- **`@DataPermission` 的实现机制**：核对是 MyBatis 拦截器还是 AOP 切面、支持的拼接方式（字段过滤/JOIN）、注解属性含义，README 描述需有代码依据。
- **filter DSL 语法与文档联动**：README 正文提到的语法摘要必须与解析器实际实现一致；可只读参考 `DevQueryVO用法.md` 是否仍描述当前实现，若不一致以源码为准修改本模块 README 的表述，并在存疑项中记录「被引用文档需另行校订」。
- **`data-changelog.enabled` 与 `data.changelog.basePackages` 命名风格并存**，务必分别核对源码，不统一改写。
- **与 JPA 模块同名的 `DataChangeLogConfiguration`**：两个模块各有一个同名不同包的装配类，README 若是"可并存"要有条件注解依据，无法确认时列入存疑项。
- **MyBatis-Plus 拦截器注册方式**：核对是否使用 `MybatisPlusInterceptor`，README 若沿用旧版 `PaginationInterceptor` 写法必须纠正。

## 执行流程
1. 完整遍历 `/workspace/travelsky_dev_mybatis_repository/src/main`（全部 `.java` 与 `resources`，含 `META-INF`），建立「代码事实清单」：装配类与 `@ConditionalOn*`、Properties 绑定结构与字段初始值、拦截器链与 `maxLimit`、注解定义、查询解析入口与支持的语法。
2. 通读 README，逐节比对；按需只读方式参考上述两个被引用文档（不得修改它们）。
3. 输出 18 项核对结论表。
4. 对每条差异给出处理结论：`修正 README` / `删除过时描述` / `存疑待人工确认`。
5. 仅修改本模块 README，最小必要改动。

## 输出要求（先清单，再改文档）
先输出：

| # | 核对项 | README 原文位置 | README 描述 | 代码实际 | 结论 |
|---|--------|-----------------|-------------|----------|------|

修改完成后输出：
- **「已修改」清单**：逐条说明改了什么、依据哪个源文件的哪段代码（给出文件路径与关键行）。
- **「存疑项」清单**：代码无法明确判断之处；若发现 `DevQueryVO用法.md` / `FILTER_USAGE_GUIDE.md` 与源码不一致，在此列出（本次不修改它们）。
- 若 18 项全部一致，明确输出「本模块 18 项均一致，README 无需修改」，并**不作任何改写**。

修订 README 时遵守：
- 保留原有章节结构、标题层级、表格风格与语气，不推倒重写、不扩充无关内容。
- 配置表维持「配置项 | 说明 | 默认值」三列格式。
- 不新增未经代码验证的能力描述；不为凑完整性编造默认值或用法。
- 概述性表述无明确错误的，不要改动。

## 硬性约束
- 事实基准只有源码；源码与 README 冲突即以源码为准。
- 不得修改任何 `.java` / `.xml` / `.yml` 文件；不得修改 `DevQueryVO用法.md` 与 `FILTER_USAGE_GUIDE.md`；本次产物只有本模块 README。
- 不得删除 README 中代码仍然存在的能力描述。
- 不输出「已全部核对完成」之类的结论性套话，用具体清单证明核对过程。
