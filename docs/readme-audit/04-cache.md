# README 一致性核对 · 04 · cache

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、Spring Cache 抽象、Hazelcast 与 Caffeine。

## 目标
核对 `cache` 的 README 中关于「依赖引入、两套缓存配置、自动装配条件、键生成规则」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/cache`
- 待核对/修订文档：`/workspace/cache/README.md`
- 处理序号：04 / 16
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`org.wymix.framework:cache:${revision}`；父 pom `dev_parent:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：多级缓存——Hazelcast 分布式缓存 + Caffeine 本地缓存，按命名空间配置过期策略
- 内部依赖：`utils`
- 外部依赖：`spring-boot-starter-cache`（排除 logging）、`hazelcast-spring 5.2.5`（**optional**）、`caffeine`（**optional**）、`junit-jupiter`(test)
- 自动装配注册：`AutoConfiguration.imports` → `org.wymix.framework.cache.configuration.CacheManagerConfiguration`
- README 声明的配置前缀：`spring.hazelcast.cache`、`caffeine`
- 源码包根：`org.wymix.framework.cache`
- 已知约束：pom 中 pitest 目标类为 `CacheManagerConfiguration*`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致
3. **README 是否明确写明 Hazelcast / Caffeine 为 `optional`，需业务方自行引入对应依赖才生效**
4. 「依赖引入即生效」表述与 `AutoConfiguration.imports` 注册方式一致；确认无 `spring.factories`

### B 自动装配与生效条件
5. README 装配类清单完整：`CacheManagerConfiguration` 及其内嵌的 `HazelcastCacheConfiguration` / `CaffeineCacheConfiguration` 全限定名正确，无遗漏的装配类
6. `@ConditionalOnClass(HazelcastInstance)` / `@ConditionalOnClass(Caffeine)` 实际是否如 README 所述；是否还存在未写入 README 的 `@ConditionalOnProperty` / `@ConditionalOnMissingBean`
7. 两套 Properties 类的 `prefix`、字段名、类型、默认值与 README 两张配置表逐条一致
8. 配置项完整性：漏列 / 多余 / 改名 / 默认值不符；特别注意 `spring.hazelcast.cache.namespace.<ns>.*` 与 `caffeine.cache.<name>.*` 的 Map 嵌套绑定写法

### C 使用方式
9. `CustomHazelcastCacheManager` 全限定名与公开方法是否与源码一致
10. 若 README 要求继承某基类或实现某接口，核对签名与泛型参数
11. README 提到的注解（标准 Spring Cache 注解）属性用法是否准确
12. 「实现 `CachingConfigurer` 接口」「可声明 `CachingConfigurer` Bean 覆盖」的说法是否有 `@ConditionalOnMissingBean` 支撑
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. yaml 片段中 `spring.hazelcast.cache.*` 与 `caffeine.cache.*` 的缩进层级是否真实可绑定

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 标记的废弃说明（如「已不再使用 `CachingConfigurerSupport`」）与代码现状一致
18. 「按命名空间配置过期策略」「多级缓存」「两者可共存」等断言是否有代码依据；无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **缓存 key 生成规则**：README 写 `<APP>:CACHE:类:方法:参数`，需与 `CustomHazelcastCacheManager` 中的实际拼接串逐字符核对（分隔符、大小写、APP 变量来源）。
- **自动补充 default 命名空间**：README 称未配置 `default` 时自动补充 `<app>:default`，TTL 86400s / LRU，需核对实际命名模板与默认值。
- **Hazelcast 三种模式枚举**：`SINGLETON` / `CLUSTER` / `CLIENT` 的枚举常量名与配置可接受字符串是否一致（大小写敏感）。
- **默认值逐条校验**：尤其 `tcpIpEnabled=true`、`publicAddress=127.0.0.1:5701`、`members=127.0.0.1`、命名空间默认 TTL 86400 / size 10000 / LRU / PER_NODE、`caffeine.cache.<name>.maximumSize=999`、`expireInSec=600`、`allowNullValues=true` 等，必须读源码字段初始值。
- **optional 依赖的生效前提**：README 若未提示"需自行引入 hazelcast / caffeine 依赖"应补充；若声称"依赖引入即自动启用"需纠正。
- **两者共存时的 CacheManager 关系**：是否真的会同时构建两个 `CacheManager`、是否存在主 `CacheManager` 选择逻辑，README 描述需有代码依据。

## 执行流程
1. 完整遍历 `/workspace/cache/src/main`（全部 `.java` 与 `resources`，含 `META-INF`、`*.yml`、`*.properties`），建立「代码事实清单」：装配类与 `@ConditionalOn*`、两套 Properties 的绑定结构（含 Map 嵌套）与字段初始值、key 生成逻辑、默认命名空间逻辑、枚举常量。
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
- 配置表维持「配置项 | 说明 | 默认值」三列格式，两套前缀分表保持独立。
- 不新增未经代码验证的能力描述；不为凑完整性编造默认值或用法。
- 概述性表述无明确错误的，不要改动。

## 硬性约束
- 事实基准只有源码；源码与 README 冲突即以源码为准。
- 不得修改任何 `.java` / `.xml` / `.yml` 文件，本次产物只有 README。
- 不得删除 README 中代码仍然存在的能力描述。
- 不输出「已全部核对完成」之类的结论性套话，用具体清单证明核对过程。
