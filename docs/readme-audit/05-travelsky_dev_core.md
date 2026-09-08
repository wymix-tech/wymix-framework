# README 一致性核对 · 05 · travelsky_dev_core

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、Spring Boot 启动生命周期（`SpringFactoriesLoader`、`EnvironmentPostProcessor`、`ApplicationListener`）、Spring AOP 与 Feign。

## 目标
核对 `travelsky_dev_core` 的 README 中关于「依赖引入、配置开关、自动装配、启动方式、横切能力」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/travelsky_dev_core`
- 待核对/修订文档：`/workspace/travelsky_dev_core/README.md`
- 处理序号：05 / 16（中枢模块，被 `repository` / `security` / `monitor` / `web` / `discovery` 依赖，改动影响面大）
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`com.travelsky.base:travelsky_dev_core:${revision}`；父 pom `travelsky_dev_parent3:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：框架启动入口与横切能力中枢——启动加载、异步调度、Feign 动态服务发现、邮件与消息发送
- 内部依赖：`travelsky_dev_common`、`travelsky_dev_threadpool`
- 外部依赖：`spring-boot-starter`（排除 logging）、`spring-boot-starter-log4j2`、`spring-boot-starter-aop`、`jakarta.servlet-api`、`spring-boot-starter-mail`（**optional**）、`feign-core` / `feign-slf4j` / `feign-jackson`、`spring-cloud-starter-openfeign`、`spring-cloud-starter-loadbalancer`、`spring-boot-starter-validation`
- 自动装配注册：`AutoConfiguration.imports` → `com.travelsky.base.core.configuration.CoreConfiguration`、`com.travelsky.base.core.configuration.AsyncScheduleAutoConfiguration`
- `spring.factories`：`org.springframework.context.ApplicationListener` → `LogApplicationStartedEventListener`、`FeignApplicationStartedEventListener`（文件注释明确「不再需要框架自定义启动入口类」）
- README 声明的开关：`travelsky.core.enabled`、`async.enabled`（均默认 `true`）；异步/调度前缀 `async`
- 源码包根：`com.travelsky.base.core`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致
3. **README 是否说明邮件能力依赖 `spring-boot-starter-mail`（optional），需业务方自行引入**
4. 「引入依赖即自动生效」与 `AutoConfiguration.imports` + `spring.factories` 双注册机制的描述一致

### B 自动装配与生效条件
5. README 装配类清单完整：`CoreConfiguration`、`AsyncScheduleAutoConfiguration` 全限定名正确，且无遗漏的装配类（源码内可能存在更多 `@Configuration`）
6. `travelsky.core.enabled`、`async.enabled` 是否真实存在；核对 `@ConditionalOnProperty` 的 `havingValue` 与 `matchIfMissing`，确认默认是否真为 `true`；本模块 summary 提到「引入依赖即自动生效，可用开关关闭」，需与条件注解逐条对齐
7. `async.*` Properties 类的 `prefix`、字段名、类型、默认值与 README 配置表逐条一致；核对是否存在 README 未提到的其他 Properties 类（邮件、Feign、日志等前缀）
8. 配置项完整性：漏列 / 多余 / 改名 / 默认值不符 / 嵌套结构不一致

### C 使用方式
9. README 示例中的工具类 / 服务类 API 签名（Feign 动态调用、邮件发送、消息发送）是否与源码一致
10. 若 README 要求继承某基类或实现某接口，核对签名与泛型参数
11. 若存在自定义注解（如异步、日志埋点、Feign 标记），核对属性名、默认值、可标注位置
12. 「声明自定义 Bean 即覆盖默认实现」的说法是否有 `@ConditionalOnMissingBean` 支撑；监听器扩展点是否说明
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. yaml 片段中 `async.*` 等前缀的缩进层级是否真实可绑定

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 若仍提到「需继承/使用框架自定义启动入口类」，与 `spring.factories` 注释声明的「原生启动方式即可生效」是否矛盾，以代码为准
18. 「引入即自动生效」「动态服务发现」「异步调度」等断言是否有代码依据；无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **双注册机制的准确表述**：`AutoConfiguration.imports`（装配 Bean）与 `spring.factories`（注册 `ApplicationListener`，监听 `ApplicationEnvironmentPreparedEvent`，发生在 ApplicationContext 创建前）职责不同，README 必须区分清楚，避免误导为「监听器可声明为 `@Bean`」。
- **启动入口类**：`spring.factories` 注释明确业务方用 `SpringApplication.run` / `SpringApplicationBuilder` / `new SpringApplication(...)` 均可生效。若 README 仍要求使用框架自定义启动类，必须删除或改写。
- **两个关闭开关的真实作用范围**：`travelsky.core.enabled` 与 `async.enabled` 分别控制哪些装配类，是否有交叉，README 描述需精确。
- **`async.*` 与 `thread-pool` 的关系**：本模块依赖 `threadpool` 模块，`async` 前缀线程池是否复用 `thread-pool` 能力；README 若混用两个前缀需纠正（跨模块行为无法确认时列入存疑项）。
- **optional 邮件依赖**：README 使用示例若直接写邮件发送，应提示前置引入条件。
- **Log4j2 与排除 logging**：README 若涉及日志配置说明，需与 pom 实际排除项一致。
- **`spring.factories` 残留**：Spring Boot 3 已废弃自动装配用的 `spring.factories`，但此处用于 `ApplicationListener` 属合法用法，README 不应将其描述为「遗留待迁移」。

## 执行流程
1. 完整遍历 `/workspace/travelsky_dev_core/src/main`（全部 `.java` 与 `resources`，重点 `META-INF/spring.factories`、`AutoConfiguration.imports`、`*.yml`、`*.properties`），建立「代码事实清单」：装配类与 `@ConditionalOn*`、Properties 绑定结构与字段初始值、监听器与监听事件类型、公开 API 签名。
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
- **「存疑项」清单**：代码无法明确判断之处（尤其是与 `threadpool` / `common` 的跨模块行为），说明为何不臆断。
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
