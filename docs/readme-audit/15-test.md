# README 一致性核对 · 15 · test

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Maven 多模块工程、JUnit 5、Testcontainers 1.20.x 与 DBUnit。

## 目标
核对 `test` 的 README 中关于「依赖引入（含 `test` scope）、测试基类清单、容器类型、Excel 数据集」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/test`
- 待核对/修订文档：`/workspace/test/README.md`
- 处理序号：15 / 16（无内部依赖，放最后统一处理）
- **同时校验测试资源**：若 `src/test` 或 `src/main/resources` 下存在示例数据集/配置文件（如 `.xlsx`），只确认其存在性与路径，不修改
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`org.wymix.framework:test:${revision}`；父 pom `dev_parent:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：测试基础设施——Testcontainers 封装（MySQL/Oracle/PostgreSQL/Kafka/Redis）与 DBUnit Excel 数据集
- 内部依赖：无
- 外部依赖：`spring-boot-starter-test`（排除 logging）、`junit-jupiter 5.7.2`、`testcontainers` BOM `1.20.6`（`testcontainers`、`junit-jupiter`、`postgresql`、`oracle-xe`、`mysql`）、`dbunit 2.8.0`、`poi 4.1.0` + `poi-ooxml 4.1.0`（排除 log4j / commons-logging）、`spring-boot-starter-jdbc`
- 自动装配：无；`AutoConfiguration.imports` / `spring.factories` 均未发现
- README 声称：无专有配置前缀，通过 Testcontainers 标准属性驱动
- 源码包根：`org.wymix.framework.test`
- 源码规模：约 21 个 `.java`（核对时以实际数量为准）

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致；**`<scope>test</scope>` 是否在 README 依赖片段中正确体现**
2. README 声明的 JDK / Spring Boot 版本与 pom 一致；Testcontainers BOM `1.20.6`、DBUnit `2.8.0`、POI `4.1.0` 版本是否与 pom 一致
3. README 是否说明运行前提：**需要可用的 Docker 环境**（Testcontainers 必需）
4. 「无自动配置、继承测试基类即可」与实际情况一致：确认无 `AutoConfiguration.imports`、`spring.factories`

### B 自动装配与生效条件
5. 源码中若实际存在 `@TestConfiguration` / `@Configuration` / Spring 上下文基类，README「集成类（测试基类清单）」表是否覆盖完整
6. 核对是否存在 `@ConditionalOn*` 或 `@EnabledIf` 类条件装配
7. 核对是否真的没有 `@ConfigurationProperties` 绑定类；README「无专有配置前缀」的结论需验证
8. 若存在自定义系统属性/环境变量（如镜像名、端口），README 是否将其与"配置项"混为一谈；需区分 PropertySource 绑定与环境变量

### C 使用方式
9. **测试基类清单与实际源码一一对应**：README 声称支持 MySQL/Oracle/PostgreSQL/Kafka/Redis，需核对每种容器是否真有对应基类/子类，类名、全限定名、`@Testcontainers` 注解用法是否一致
10. `AbstractContainer` 的抽象方法、泛型参数、继承方式与子类方法签名是否一致；`@Container` 静态字段的写法是否可编译
11. 若存在自定义注解，核对属性名、默认值、可标注位置
12. 扩展点（自定义容器、自定义镜像名、自定义初始化脚本挂载）是否有代码依据
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. 若 README 给出 yaml/properties 片段，是否真实生效（例如 `DOCKER_HOST` 属环境变量配置项，不应写在 `application.yml` 里）

### D 其他描述
16. README 引用的文件路径、类名、文档锚点（包括示例 `.xlsx` 数据集路径）是否仍存在
17. README 标记为「已废弃」的基类与代码现状一致
18. 「接近生产环境」「降低数据准备成本」等断言是否有代码依据；无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **容器种类是否齐全**：README 列出 MySQL / Oracle / PostgreSQL / Kafka / Redis，需逐个确认 pom 中是否真有对应 Testcontainers 模块依赖、源码中是否真有对应封装类（pom 中目前仅见 postgresql / oracle-xe / mysql，**Kafka 与 Redis 是否真实存在需重点核实**）。
- **`AbstractContainer` 继承方式**：核对是 JUnit 5 的 `@Testcontainers` + `@Container` 静态字段，还是自己管理生命周期（`start()` / `stop()`），README 示例必须与之一致。
- **Excel 数据集 API**：DBUnit 相关工具类的类名、方法签名、Excel 文件结构约定（sheet 名、首行列名）是否与 README 示例一致；示例文件路径是否存在。
- **POI 版本 4.1.0 的 API 差异**：若 README 示例使用新版 POI 写法（如 `XSSFWorkbook` 相关 API）需核对兼容性。
- **`junit-jupiter 5.7.2` 显式版本**：与 Spring Boot 3.5.8 管理的 JUnit 版本不同，README 若声称由 Spring Boot 统一管理需纠正。
- **排除 logging**：模块排除了 `spring-boot-starter-logging`，若 README 涉及日志依赖说明需一致。

## 执行流程
1. 完整遍历 `/workspace/test/src`（`main` 与 `test` 下的 `.java` 与 `resources`），建立「代码事实清单」：基类/子类清单（含全限定名）、容器类型与镜像名、生命周期注解用法、DBUnit/Excel 工具类方法签名、示例文件路径是否存在。
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
- **「存疑项」清单**：代码无法明确判断之处（如某容器类缺失是否属于计划支持），说明为何不臆断。
- 若 18 项全部一致，明确输出「本模块 18 项均一致，README 无需修改」，并**不作任何改写**。

修订 README 时遵守：
- 保留原有章节结构、标题层级、表格风格与语气，不推倒重写、不扩充无关内容。
- 若需新增配置/属性表，维持「配置项 | 说明 | 默认值」三列格式。
- 不新增未经代码验证的能力描述；不支持的容器类型必须从清单中删除，不得保留。
- 概述性表述无明确错误的，不要改动。

## 硬性约束
- 事实基准只有源码；源码与 README 冲突即以源码为准。
- 不得修改任何 `.java` / `.xml` / `.yml` 文件，本次产物只有 README。
- 不得删除 README 中代码仍然存在的能力描述。
- 不输出「已全部核对完成」之类的结论性套话，用具体清单证明核对过程。
