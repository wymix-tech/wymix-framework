# README 一致性核对 · 10 · monitor

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、Spring Boot Actuator 与 Micrometer/Prometheus。

## 目标
核对 `monitor` 的 README 中关于「依赖引入、默认注入的 Actuator 配置、启动监听器机制」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/monitor`
- 待核对/修订文档：`/workspace/monitor/README.md`
- 处理序号：10 / 16
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`org.wymix.framework:monitor:${revision}`；父 pom `dev_parent:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：基于 Actuator + Micrometer 的应用可观测性——指标暴露与启动事件采集
- 内部依赖：`core`
- 外部依赖：`spring-boot-starter-actuator`（排除 logging）、`micrometer-registry-prometheus`
- 自动装配注册：`AutoConfiguration.imports` → `org.wymix.framework.monitor.configuration.MonitorConfiguration`
- `spring.factories`：`org.springframework.context.ApplicationListener` → `org.wymix.framework.monitor.listener.MonitorApplicationStartedEventListener`；文件注释写明**该监听器内部会读取 `wymix.monitor.enabled`（默认 `true`）决定是否注入 Actuator 默认值**
- README 声称：本模块无专有配置前缀，使用 Actuator / Micrometer 标准配置
- 源码包根：`org.wymix.framework.monitor`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致
3. README 是否说明 Actuator 端点暴露的前置条件（如是否依赖 `spring-boot-starter-web` 才有 HTTP 端点）
4. 「业务方使用原生启动方式即可生效」的表述与 `spring.factories` 注册 `ApplicationListener` 的机制一致；`AutoConfiguration.imports` 与 `spring.factories` 的职责是否被准确区分

### B 自动装配与生效条件
5. README 装配类清单完整：`MonitorConfiguration` 全限定名正确，且无遗漏的装配类
6. **README 提到「启用 `MonitorBootLoader` 后注入默认值」，需核对 `MonitorBootLoader` 是否仍然存在**；实际注入逻辑是否在 `MonitorApplicationStartedEventListener` 中；`wymix.monitor.enabled`（默认 `true`）是否真实存在，核对读取方式与 `matchIfMissing` 默认值
7. 若存在 Properties 绑定类，核对 `prefix`、字段、默认值；README「无专有配置前缀」的结论是否成立（至少存在 `wymix.monitor.enabled` 这类零散开关）
8. **默认注入项逐条核对**：README 列出的 5 项默认值必须与监听器实际注入的 key/value 完全一致
   - `management.endpoints.web.exposure.include` = `*`
   - `management.endpoint.health.showDetails` = `always`
   - `management.endpoint.prometheus.enabled` = `true`
   - `management.metrics.export.prometheus.enabled` = `true`
   - `management.metrics.tags.application` = `${spring.application.name:app}`

### C 使用方式
9. README 示例（自定义指标、MeterRegistry 注入、端点访问路径）的类名、全限定名、方法签名是否与源码一致
10. 若 README 要求继承某基类或实现某接口，核对签名与泛型参数
11. 若存在自定义注解（如埋点计数），核对属性名、默认值、可标注位置
12. README 提到的扩展点（自定义 Listener、自定义指标注册）是否有 `@ConditionalOnMissingBean` 或 SPI 依据
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. 若有 yaml 片段，缩进与前缀是否真实可绑定；「因注入源在最后，业务配置可覆盖」的说法需核对 **PropertySource 加入顺序**是否真的如此

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 标记为「已废弃」的类（如疑似已不存在的 `MonitorBootLoader`）与代码现状一致
18. 「暴露线程池/JVM/HTTP 指标」「无需自定义启动入口」等断言是否有代码依据；无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **`MonitorBootLoader` 是否存在**：README 把「注入默认值」的行为描述为"启用 `MonitorBootLoader` 后"，而实际实现可能是 `MonitorApplicationStartedEventListener`（`ApplicationListener`）。类名若已变更或已删除，README 必须按代码改写。
- **默认值的覆盖优先级**：监听器通过向 `Environment` 添加 `PropertySource` 实现默认值注入，README 声称「业务配置可覆盖（因注入源在最后）」，需核对插入位置（`addFirst` / `addLast`）与优先级语义是否一致。
- **`wymix.monitor.enabled` 前缀归属**：模块名前缀 `wymix.monitor` 与「无专有配置前缀」的表述冲突，需以代码为准统一。
- **监听的事件类型**：核对监听 `ApplicationEnvironmentPreparedEvent` 还是 `ApplicationStartedEvent`，README 描述的触发时机需一致。
- **Actuator 端点路径**：若 README 给出访问路径示例，核对是否受 `management.endpoints.web.base-path` 影响。
- **Prometheus 端点可用性**：`micrometer-registry-prometheus` 为 compile 依赖，README 若声称"开箱暴露 /actuator/prometheus"需验证。

## 执行流程
1. 完整遍历 `/workspace/monitor/src/main`（全部 `.java` 与 `resources`，重点 `META-INF/spring.factories`、`AutoConfiguration.imports`），建立「代码事实清单」：装配类、`ApplicationListener` 实现与监听事件、注入的 PropertySource 键值 `Map`、开关读取方式、Provided Beans。
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
- 若需新增配置表，维持「配置项 | 说明 | 默认值」三列格式。
- 不新增未经代码验证的能力描述；不为凑完整性编造默认值或用法。
- 概述性表述无明确错误的，不要改动。

## 硬性约束
- 事实基准只有源码；源码与 README 冲突即以源码为准。
- 不得修改任何 `.java` / `.xml` / `.yml` 文件，本次产物只有 README。
- 不得删除 README 中代码仍然存在的能力描述。
- 不输出「已全部核对完成」之类的结论性套话，用具体清单证明核对过程。
