# README 一致性核对 · 09 · travelsky_dev_security

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、Spring Security 6.x、JWT（jjwt 0.13.0）与配置加密机制。

## 目标
核对 `travelsky_dev_security` 的 README 中关于「依赖引入、配置前缀、自动装配、`ENC()` 解密、JWT 黑名单」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/travelsky_dev_security`
- 待核对/修订文档：`/workspace/travelsky_dev_security/README.md`
- 处理序号：09 / 16（`web` 模块依赖本模块，先做本模块）
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`com.travelsky.base:travelsky_dev_security:${revision}`；父 pom `travelsky_dev_parent3:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：认证与访问控制——JWT 无状态认证、令牌黑名单、配置项 `ENC()` 解密、BCrypt 加密
- 内部依赖：`travelsky_dev_core`
- 外部依赖：`spring-boot-starter-security`（排除 logging）、`jjwt-api` / `jjwt-impl`(runtime) / `jjwt-jackson`(runtime)，版本 `0.13.0`
- 自动装配注册：`AutoConfiguration.imports` → `com.travelsky.base.security.configuration.CustomSecurityConfiguration`
- `spring.factories`：`org.springframework.boot.env.EnvironmentPostProcessor` → `com.travelsky.base.security.handler.DecryptEnvironmentPostProcessor`（文件注释明确：`EnvironmentPostProcessor` 只能在此注册，不可迁移到 `imports`）
- README 声明的配置前缀：`security`
- 源码包根：`com.travelsky.base.security`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致；**jjwt 版本 `0.13.0` 是否被正确说明**（0.12+ 起 API 与旧版不兼容，README 示例若用旧 API 需纠正）
3. README 是否说明「只需提供 `UserDetailsService` Bean 即启用安全」的前置条件；是否说明表单登录与 JWT 两种模式需要不同的依赖/配置
4. 「引入即自动生效」与 `AutoConfiguration.imports` 注册方式一致；`ENC()` 解密走 `spring.factories` 的机制是否被准确描述

### B 自动装配与生效条件
5. README 装配类清单完整：`CustomSecurityConfiguration` 全限定名正确，且无遗漏的装配类（JWT、黑名单等可能有独立装配类）
6. `security.enabled`（README 称默认 `true`）是否真实存在；核对 `@ConditionalOnProperty` 的 `havingValue` 与 `matchIfMissing`；是否还有 `@ConditionalOnClass` / `@ConditionalOnMissingBean`（如「提供 `UserDetailsService` 才启用」的说法需有 `@ConditionalOnBean` 依据）
7. Security Properties 类的 `prefix`、字段名、类型、默认值与 README 配置表逐条一致（注意字段名混合命名，如 `formLoginEnable` 而非 `formLogin.enabled`）
8. 配置项完整性：漏列 / 多余 / 改名 / 默认值不符 / 嵌套结构不一致；特别注意 `ignoreUrls` 是 `List` 还是逗号分隔字符串

### C 使用方式
9. README 示例（JWT 生成/校验、黑名单操作、加解密工具类）的类名、全限定名、方法名、参数与返回值是否与源码一致
10. 若 README 要求继承某基类或实现某接口（如自定义认证过滤器、`UserDetailsService`），核对签名与泛型参数
11. 若存在自定义注解（如放行标记、权限标记），核对属性名、默认值、可标注位置
12. 「提供自定义 Bean 即覆盖默认实现」的说法是否有 `@ConditionalOnMissingBean` 支撑；过滤器链扩展点（如 `SecurityFilterChain` Bean）说明是否准确
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. yaml 片段中 `security.*` 前缀与缩进是否真实可绑定；`ENC(...)` 写法是否与解密处理器识别的前后缀一致

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 若仍提到继承 `WebSecurityConfigurerAdapter`（Spring Security 6 已移除）或旧版配置方式，必须以代码为准纠正
18. 「新版无需继承配置类」「TOKEN 黑名单主动失效」「`ENC()` 自动解密」等断言是否有代码依据；无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **Spring Security 6 API**：核对是 `SecurityFilterChain` Bean + `SecurityFilterChain` lambda 配置，还是已废弃的 `WebSecurityConfigurerAdapter`；README 示例必须能编译。
- **`ENC()` 解密边界**：核对识别的前缀（是否仅 `ENC(`）、是否对所有 `PropertySource` 生效、解密失败时的行为；README 若声称"配置文件所有位置均可使用"需验证。
- **JWT 黑名单的存储与清理**：核对存储介质（内存/Cache/Redis）、TTL 语义（是否等于 token 剩余有效期）、是否有过期清理线程，README 若声称"支持分布式"需有代码依据。
- **jjwt 0.13.0 API**：核对 `Jwts.builder()` / `parserBuilder()` 等调用签名；若 README 用 `Jwts.parser().setSigningKey()`（旧版写法）必须纠正，同时注意 0.12+ 解析 API 变化（`verifyWith` vs `setSigningKey`）。
- **字段名拼写**：`formLoginEnable`、`loginPage`、`loginProcessingUrl`、`logoutUrl`、`ignoreUrls`、`enabled` 必须逐字与 Properties 字段一致，勿按 Spring 惯例推断。
- **与 `web` 模块的交互**：`web` 依赖本模块，README 若描述放行/鉴权与 Web 层的协作，跨模块无法确认时列入存疑项。

## 执行流程
1. 完整遍历 `/workspace/travelsky_dev_security/src/main`（全部 `.java` 与 `resources`，重点 `META-INF/spring.factories`、`AutoConfiguration.imports`），建立「代码事实清单」：装配类与 `@ConditionalOn*`、Properties 绑定结构与字段初始值、过滤器链构成、`ENC` 解密处理器逻辑、JWT 工具类方法签名、黑名单实现。
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
- **「存疑项」清单**：代码无法明确判断之处（尤其与 `web` 模块的协作行为），说明为何不臆断。
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
