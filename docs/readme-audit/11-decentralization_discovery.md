# README 一致性核对 · 11 · decentralization_discovery

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、配置驱动的服务发现与分布式节点协调机制（Leader 选举、心跳保活）。

## 目标
核对 `decentralization_discovery` 的 README 中关于「依赖引入、配置前缀、自动装配、Leader 选举与心跳保活」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/decentralization_discovery`
- 待核对/修订文档：`/workspace/decentralization_discovery/README.md`
- 处理序号：11 / 16
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`org.wymix.framework:decentralization_discovery:${revision}`；父 pom `dev_parent:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：配置驱动的去中心化服务发现——集群成员由 `decentralization.discovery.service.<svc>` 显式配置（**不做网络自动发现**，原 JmDNS/mDNS 已移除），内置节点注册、Leader 选举、心跳保活与广播事件
- 内部依赖：`core`
- 外部依赖：`spring-boot-starter-actuator`（排除 logging）、`feign-httpclient 13.1`（无 optional 依赖）
- 自动装配注册：`AutoConfiguration.imports` → `org.wymix.framework.discovery.configuration.DecentralizedDiscoveryConfiguration`
- README 声明的配置前缀：`decentralization.discovery`
- 源码包根：`org.wymix.framework.discovery`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致
3. **README 是否说明「无 optional 依赖、不做自动发现」**；是否写明节点只来自 `service.<svc>` 配置与其它实例主动 `nodeRegister`
4. 「引入即生效 / 需开关」的表述与 `AutoConfiguration.imports` 注册方式一致；确认无 `spring.factories`

### B 自动装配与生效条件
5. README 装配类清单完整：`DecentralizedDiscoveryConfiguration` 全限定名正确，且无遗漏的装配类（节点注册、Leader 选举、心跳等可能有独立配置类）
6. `decentralization.discovery.enable`（README 称默认 `false`）是否真实存在，**注意字段名是 `enable` 还是 `enabled`**；核对 `@ConditionalOnProperty` 的 `havingValue` 与 `matchIfMissing`；确认已无 `@ConditionalOnClass(JmDNS)` 残留
7. Properties 类的 `prefix`、字段名、类型、默认值与 README 配置表逐条一致
8. 配置项完整性：漏列 / 多余 / 改名 / 默认值不符；特别注意 `service.<svc>` 的类型（`List<String>` 还是逗号分隔字符串）与 `<svc>` 取值规则

### C 使用方式
9. README 示例（服务注册、服务发现、Leader 查询）的类名、全限定名、方法名、参数与返回值是否与源码一致
10. 若 README 要求继承某基类或实现某接口，核对签名与泛型参数
11. 若存在自定义注解（如标记广播事件监听），核对属性名、默认值、可标注位置
12. README 提到的扩展点（自定义事件监听、自定义 Leader 选举策略）是否有接口/SPI 或 `@ConditionalOnMissingBean` 依据
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. yaml 片段中 `decentralization.discovery.*` 前缀与 `service.<svc>` 嵌套缩进是否真实可绑定

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 标记为「已废弃」的 API 与代码现状一致
18. **「内置 Leader 选举」「心跳保活」「广播事件机制」必须有明确代码依据**：若无对应实现，删除或改为中性描述；「免去 Eureka/Nacos 等外部依赖」「适合内网小规模场景」等断言同样需核对

## 本模块重点核对项（易错点）
- **`enable` vs `enabled`**：README 写 `decentralization.discovery.enable`（非常规命名），必须与 Properties 字段名逐字核对，不要顺手改成 `enabled`。
- **无 optional 依赖**：模块已移除 JmDNS，不应再有「需显式引入 `jmdns`」类表述；`enable=false` 时是否连 Bean 都不创建，需核对条件注解组合。
- **命名空间用途**：`namespace` 仅写入 `NodeInfo.namespace`（区分环境/集群），不再拼 mDNS 服务类型，README 若仍写 `_<ns>._tcp.local.` 应修订。
- **Leader 选举实现真实性**：这是最容易与实际不符的断言，必须找到具体实现类才能保留描述；仅有接口无实现时应如实说明。
- **心跳保活周期**：是否可配置（若有周期类配置项，README 配置表是否遗漏）、默认多少秒。
- **发现结果的缓存与刷新**：是否有本地缓存、刷新间隔、失效处理；README 若声称「实时发现」需核对。

## 执行流程
1. 完整遍历 `/workspace/decentralization_discovery/src/main`（全部 `.java` 与 `resources`，含 `META-INF`），建立「代码事实清单」：装配类与 `@ConditionalOn*`、Properties 绑定结构与字段初始值、服务注册/发现 API、Leader 选举与心跳实现类、事件定义与监听器。
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
