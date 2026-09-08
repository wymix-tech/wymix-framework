# README 一致性核对 · 01 · travelsky_dev_utils

> 本文件自包含，整段复制给 AI 即可执行（以本仓库为工作区）。

## 角色
严谨的技术文档审校工程师，熟悉 Maven 多模块工程与 Java 生态，了解 Spring Boot 3.x 自动装配机制。

## 目标
核对 `travelsky_dev_utils` 的 README 中关于「引入方式、工具能力、调用方式」的描述，是否与**当前源码**一致。不一致处**一律以源码为准**修订 README。禁止为迁就文档而修改代码。

## 范围（严格限定）
- 组件目录：`/workspace/travelsky_dev_utils`
- 待核对/修订文档：`/workspace/travelsky_dev_utils/README.md`
- 处理序号：01 / 16（底层模块，结果会被上层模块 README 间接引用）
本次只处理本模块，不得跨模块读取推断；须跨模块判断的列入「存疑项」。

## 模块概况（初步定位用，不是事实基准）
- Maven 坐标：`com.travelsky.base:travelsky_dev_utils:${revision}`；父 pom `travelsky_dev_parent3:3.0.0-SNAPSHOT`；Java 17；Spring Boot 3.5.8
- 模块职责：全框架最底层通用工具集，README 自述「无自动配置、纯静态工具类」
- 内部依赖：无（被其余绝大多数模块依赖）
- 外部依赖：`commons-lang3`、`reflections 0.10.2`、`transmittable-thread-local 2.14.5`、`jackson-core/databind`、`jackson-datatype-joda`、`jackson-datatype-jsr310`、`spring-beans`、`jakarta.servlet-api`、`guava 32.1.3-jre`
- 自动装配：README 声称无；`src/main/resources/META-INF` 下未发现注册文件
- 源码包根：`com.travelsky.base.utils`
- 已知约束：pom 中配置了 pitest，目标类 `JsonDeserialize` / `ReflectUtils` / `CompressUtil`

## 核对清单（18 项，逐项给结论，无差异也要写「一致」）

### A 接入与依赖
1. `groupId` / `artifactId` / `version` 占位符与 `pom.xml` 实际坐标一致
2. README 声明的 JDK / Spring Boot 版本与 pom 一致
3. README 是否说明了需要外部 jar 能力的**前置条件**（如 `optional` 或需业务方自行引入的依赖）
4. 「引入即生效 / 需额外开关」表述与实际情况一致；确认 `src/main/resources/META-INF` 下确实没有 `AutoConfiguration.imports`、`spring.factories`

### B 自动装配与生效条件
5. 若源码中实际存在 `@Configuration` / `@AutoConfiguration` 类而 README 说「无自动配置」，以代码为准补充
6. 核对是否存在 `@ConditionalOn*` 条件装配（本模块预期无）
7. 核对是否存在 `@ConfigurationProperties` 绑定（README 声称「无外部配置项」，需验证是否真的没有任何 Properties 类）
8. 若存在配置项，README「本模块没有外部配置项」的结论需推翻并补全表格

### C 使用方式
9. README「工具类清单」表格中的类名、全限定名、方法名、参数与返回值、是否静态、`public` 可见性，是否与源码一致
10. 源码中存在但 README 未列出的工具类 → 补充；README 列出但源码已删除/改名/降级为非 public 的 → 修正或删除
11. 若存在自定义注解，核对属性名、默认值、可标注位置
12. 若 README 提到可替换/可扩展实现（如自定义序列化器），是否有对应的接口或 SPI 依据
13. README 提到的包路径迁移、旧别名是否仍成立
14. 示例代码能否编译、import 路径是否真实存在
15. 若 README 给出配置片段，缩进与前缀是否真实有效

### D 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍存在
17. README 标记为「已废弃/计划移除」的内容与代码现状一致
18. 「线程安全」「零依赖」「不依赖 Spring 容器」等断言是否有代码依据；无依据的绝对化措辞改为中性描述或删除

## 本模块重点核对项（易错点）
- **工具类清单与实际源码一一对应**：README 表格容易滞后，遗漏新增工具类或保留已删除类（如 JSON / Excel / 压缩 / 反射等分类）。
- **静态方法语义**：README 若写工具方法，需核对是 `static` 还是需要实例化；Lombok `@UtilityClass`、私有构造器等都可能影响「直接调用」的说法。
- **是否真的不依赖 Spring 容器**：源码中若出现 `SpringBeanUtils`、`ApplicationContextAware`、工具类依赖 `Environment` 等，README「不依赖 Spring 容器」的表述需修正。
- **TTL 相关能力**：`transmittable-thread-local` 已在本 pom 中，若 README 提到上下文透传，核对实际封装的方法签名与用法。
- **Jackson 模块注册**：`jackson-datatype-joda` / `jsr310` 相关的时间序列化工具方法签名与默认格式（时区、日期 pattern）是否与 README 示例输出一致。
- **Guava / Reflections 依赖**：若某工具类使用了 guava 或 reflections，README 是否说明了版本或引入前提。

## 执行流程
1. 完整遍历 `/workspace/travelsky_dev_utils/src/main`（全部 `.java` 与 `resources`），建立「代码事实清单」：类清单（含全限定名、可见性、是否静态）、公共方法签名、注解、异常行为。
2. 通读 README，逐节与事实清单比对。
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
