# README 一致性核对 · 12 · web

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。
> 前置：请先完成 05 `core` 与 09 `security`，本模块依赖二者。

## 角色
严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配、Spring MVC、springdoc-openapi 2.x 与 Jackson 序列化定制。

## 目标
核对 `web` 的 README 中关于「依赖引入、Web 配置项、自动装配、控制器基类、横切能力」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/web`
- 待核对/修订文档：`/workspace/web/README.md`
- 处理序号：12 / 16
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`org.wymix.framework:web:${revision}`；父 pom `dev_parent:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：Web 层通用能力——统一响应、TraceId 透传、Long→String 防精度丢失、i18n、慢请求监控、CRUD 控制器基类、Swagger 与时区配置
- 内部依赖：`core`、`security`
- 外部依赖：`spring-boot-starter-web`（**排除 logging 与 tomcat**）、`spring-boot-starter-undertow`、`springdoc-openapi-starter-webmvc-ui 2.8.15`、`spring-boot-starter-test`(test)
- 自动装配注册：`AutoConfiguration.imports` → `org.wymix.framework.web.configuration.WebConfiguration`、`org.wymix.framework.web.configuration.ApiDocAutoConfiguration`
- README 声明的配置项：`swagger.enabled`（默认 `true`）、`swagger.web.enabled`（默认 `false`）、`app.slow.request.inSecond`（默认 `5`）、`spring.validate.messages`（默认 `i18n/validation/ValidationMessages`）、`user.timezone`（默认 `GMT+08`）
- 源码包根：`org.wymix.framework.web`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致；springdoc 版本 `2.8.15` 是否被正确说明
3. **README 是否说明本模块使用 Undertow 替代 Tomcat**（pom 排除了 `spring-boot-starter-tomcat`），以及这一事实对业务方的影响
4. 「引入即生效」的表述与 `AutoConfiguration.imports` 注册方式一致；确认无 `spring.factories`

### B 自动装配与生效条件
5. README 装配类清单完整：`WebConfiguration`、`ApiDocAutoConfiguration` 全限定名正确，且无遗漏的装配类
6. **两级 Swagger 开关的真实语义**：`swagger.enabled` 与 `swagger.web.enabled` 分别控制什么（尤其 `swagger.web.enabled` 默认 `false` 的含义），需核对 `@ConditionalOnProperty` 的 `havingValue` 与 `matchIfMissing`，README 描述必须与其完全一致
7. Properties 类的 `prefix`、字段名、类型、默认值与 README 配置表逐条一致；注意混杂了自有前缀（`swagger.*`、`app.slow.request.*`、`user.timezone`）与 Spring 标准前缀（`spring.validate.messages`）
8. 配置项完整性：漏列 / 多余 / 改名 / 默认值不符 / 嵌套结构不一致；`spring.validate.messages` 是否为数组类型需核对

### C 使用方式
9. `ControllerResponseAdvice`、`SlowRequestAspect`、`AbstractBaseController` 等类的全限定名、泛型参数、方法签名是否与源码一致
10. `AbstractBaseController` 的继承写法与泛型参数个数（实体/ID/Service）是否与 README 示例一致；示例中的 `@RequestMapping`、路径写法是否可运行
11. 若存在自定义注解（如忽略统一包装、慢请求排除），核对属性名、默认值、可标注位置
12. 「声明自定义 Bean 即覆盖默认实现」的说法是否有 `@ConditionalOnMissingBean` 支撑；Jackson 定制、`HandlerInterceptor` 扩展点是否说明清楚
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. yaml 片段中各前缀的缩进层级是否真实可绑定

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 标记为「已废弃」的 API（如旧 Controller 基类、旧 Swagger 配置方式）与代码现状一致
18. 「TraceId 在异步线程中透传」「Long 序列化为字符串避免 JS 精度丢失」「i18n 国际化」等断言是否有代码依据；无依据的改为中性描述或删除

## 本模块重点核对项（易错点）
- **双 Swagger 开关**：`swagger.enabled=true` 但 `swagger.web.enabled=false` 的组合非常反直觉，必须精确描述两者分别控制哪些 Bean，避免业务方配置后不生效。
- **容器替换**：pom 排除了 Tomcat 并引入 Undertow，README 若有部署相关说明需与此一致。
- **统一响应包装的边界**：核对哪些返回值会被包装（是否有排除策略、是否处理 `String`、是否放行 `ResponseEntity`、是否有 `@IgnoreWrap` 类注解），README 描述需有代码依据。
- **Long→String 的实现方式**：核对是全局 `Jackson2ObjectMapperBuilder` 定制还是 `Module` 注册，是否只作用于序列化，以及是否影响参数反序列化。
- **慢请求阈值**：`app.slow.request.inSecond` 默认 5（秒），需核对单位与比较方式（>`>` 还是 `>=`）、输出日志级别是否为 WARN、是否包含异常请求。
- **时区配置**：`user.timezone` 声称在启动时设置，需核对实际是在静态块/监听器/装配类中设置 `TimeZone.setDefault` 与否。
- **与安全模块的协作**：`security` 是本模块 compile 依赖，若 README 涉及鉴权放行，跨模块无法确认时列入存疑项。

## 执行流程
1. 完整遍历 `/workspace/web/src/main`（全部 `.java` 与 `resources`，含 `META-INF`、`i18n` 资源），建立「代码事实清单」：装配类与 `@ConditionalOn*`、Properties 绑定结构与字段初始值、切面/拦截器/`ControllerAdvice`、Jackson 定制点、基类签名、i18n 资源实际路径。
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
- **「存疑项」清单**：代码无法明确判断之处（尤其与 `security` / `core` 的跨模块行为），说明为何不臆断。
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
