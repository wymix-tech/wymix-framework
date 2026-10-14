# README 一致性核对 · 02 · threadpool

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、Maven 多模块工程与 Java 并发编程。

## 目标
核对 `threadpool` 的 README 中关于「接入、线程池配置、注册中心 API、TTL 透传」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/threadpool`
- 待核对/修订文档：`/workspace/threadpool/README.md`
- 处理序号：02 / 16
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`org.wymix.framework:threadpool:${revision}`；父 pom `dev_parent:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：统一线程池注册中心，支持运行时监控与参数调整，内置 TTL 上下文透传
- 内部依赖：无（被 `core` 依赖）
- 外部依赖：`spring-boot-starter`（排除了 `spring-boot-starter-logging`）、`transmittable-thread-local 2.14.5`
- 自动装配注册：`AutoConfiguration.imports` → `org.wymix.framework.tp.configuration.ThreadPoolConfiguration`
- README 声明的开关：`wymix.threadpool.enabled`（默认 `true`）；配置前缀 `thread-pool`
- 源码包根：`org.wymix.framework.tp`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致
3. TTL 依赖是否说明（是本模块的**编译期强依赖**还是需业务方额外引入）
4. 「引入即生效」表述与 `AutoConfiguration.imports` 注册方式一致；确认无 `spring.factories`

### B 自动装配与生效条件
5. README「集成类/自动装配」表中的 `ThreadPoolConfiguration` 全限定名正确，且无遗漏的其他装配类
6. `wymix.threadpool.enabled` 开关真实存在：核对 `@ConditionalOnProperty` 的 `name`、`havingValue`、`matchIfMissing`，确认默认是否真为 `true`
7. `ThreadPoolProperties`（或等价绑定类）的 `prefix`、字段名、类型、默认值与 README 配置表逐条一致
8. 配置项完整性：漏列 / 多余 / 改名 / 默认值不符 / 嵌套结构不一致；特别注意 `thread-pool.pool.<name>.*` 的 Map 绑定写法

### C 使用方式
9. `ThreadPoolRegistry` 等公开 API 的类名、包名、方法名、参数与返回值是否与源码一致；获取线程池的方式（按名称获取 / 注入 Bean）
10. 若 README 要求继承某基类或实现某接口，核对签名与泛型参数
11. 若存在自定义注解（如标记异步执行、标记使用某线程池），核对属性名、默认值、可标注位置
12. 「运行时查看 / 调整线程池参数」的 API 是否存在且签名一致；状态返回对象字段是否与 README 示例一致
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. yaml 片段的 `thread-pool` 前缀与 `pool.<name>` 嵌套缩进是否真实可绑定

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 标记为「已废弃」的 API 与代码现状一致
18. 「自动透传 TTL 上下文」「子线程继承 traceId」等断言是否有代码依据（是否有 TTL 包装逻辑），无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **`thread-pool.pool.<name>.maxPoolSize` 默认值**：README 写「2×corePoolSize」，需核对代码里是静态默认值还是计算派生，描述是否准确。
- **缺省线程池**：若未配置任何 `pool.<name>`，是否有默认线程池、名称与参数是什么，README 是否遗漏。
- **拒绝策略**：README 若列出可选拒绝策略枚举，核对枚举常量名与实际取值字符串（配置项往往用字符串绑定）。
- **TTL 透传的覆盖面**：核对是「所有托管线程池」还是仅部分包装；`core` 模块的异步线程池是否复用本模块能力（属跨模块，列入存疑项即可）。
- **运行时调参的边界**：是否允许调整核心线程数/最大线程数、是否持久化、是否存在校验限制（如 maxPoolSize 必须 ≥ corePoolSize），README 若声称可无限制调整需修正。
- **开关命名差异**：`wymix.threadpool.enabled` 与前缀 `thread-pool` 命名风格不一致，务必确认代码里的真实写法，勿按惯例推断。

## 执行流程
1. 完整遍历 `/workspace/threadpool/src/main`（全部 `.java` 与 `resources`，含 `META-INF`），建立「代码事实清单」：装配类与条件、Properties 绑定结构（含 Map 嵌套）、公开 API 签名、枚举常量。
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
- **「存疑项」清单**：代码无法明确判断之处，说明为何不臆断。
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
