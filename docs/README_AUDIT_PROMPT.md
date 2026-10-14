# 组件 README 一致性核对提示词（单模块逐个处理）

> 使用方式：**一次只处理一个模块**，每个模块单独跑一轮，单独产出一份核对清单。
> 使用前把下面 `{{MODULE_DIR}}` 与 `{{README_PATH}}` 替换为实际路径，整段复制给 AI 执行。

---

## 提示词正文（复制以下内容）

```
# 任务：单组件 README 与代码实现一致性核对与同步

## 角色
你是一名严谨的技术文档审校工程师，熟悉 Spring Boot 3.x 自动装配机制、Maven 多模块工程结构与 Java 生态惯例。

## 目标
核对组件 README 中关于「接入方式、配置能力、使用方式」的描述，是否与当前组件源码的真实实现一致。
若存在不一致，一律以当前组件代码为准修正 README。禁止为了让文档与代码"看起来一致"而修改代码。

## 本次范围（严格限定）
- 组件目录：{{MODULE_DIR}}
- 待核对/修订文档：{{README_PATH}}
本次只处理上述单个模块。不得读取其他模块代码来交叉推断本模块行为，不得修改本模块以外的任何文件。
若某结论必须依赖其他模块才能判断，列入「存疑项」交由人工确认，不要自行推断。

## 核对清单（逐项读完源码后再判定，不得凭 README 自我印证）

### A. 接入与依赖
1. Maven 依赖：groupId / artifactId / version 占位符是否与 {{MODULE_DIR}}/pom.xml 及父 pom 的实际坐标一致。
2. 版本要求：README 声明的 JDK / Spring Boot 版本是否与 pom.xml 中 java.version 及依赖版本一致。
3. 前置依赖：README 声称的可选依赖、传递依赖是否真实存在；若某能力依赖外部 jar，README 是否说明了该前提。
4. 自动装配注册：检查 resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
   （以及是否仍残留旧版 spring.factories），README 所说"引入即生效 / 需 @EnableXxx"是否与注册方式一致。

### B. 自动装配与生效条件
5. 配置类清单：README「集成类/自动装配」表中的类是否存在、全限定名是否正确；有无文档未提及的
   @Configuration / @AutoConfiguration 类。
6. 生效条件：逐个核对 @ConditionalOnClass / @ConditionalOnProperty / @ConditionalOnMissingBean /
   @ConditionalOnBean，README 中"条件装配""默认开启/关闭"是否准确；
   特别注意 @ConditionalOnProperty 的 havingValue 与 matchIfMissing 组合。
7. Property 绑定：@EnableConfigurationProperties 声明的 Properties 类与 @ConfigurationProperties(prefix = "...")
   的前缀、字段名、类型、默认值是否与 README 配置项表格逐条一致。
8. 配置项完整性（README 表格 vs Properties 类字段）：
   - 代码有但 README 未列出 → 补充；
   - README 列出但代码已删除/改名 → 删除或改为实际名称；
   - 默认值不符 → 以代码字段初始值为准；
   - 嵌套结构（如 namespace.<ns>.xxx、Map/List）写法是否与实际绑定结构一致。

### C. 使用方式
9. API 签名：README 示例中的类名、包名、方法名、参数个数与类型、返回值是否与源码一致；
   构造方式（构造器注入 vs new）是否可行。
10. 继承/实现关系：README 声称的抽象类、基类、接口及其泛型参数个数
    （如 AbstractBaseServiceImpl<T, ID, REPOSITORY>）是否正确。
11. 自定义注解：属性名、default 值、可标注位置是否一致。
12. 扩展点/覆盖方式：README 声明的"声明自定义 Bean 即覆盖默认实现"是否与 @ConditionalOnMissingBean 一致；
    SPI 接口、事件监听器的注册方式是否描述准确。
13. 包路径迁移说明：README 提到的包路径迁移、旧别名是否仍成立（旧兼容类是否已被删除）。
14. 示例代码可编译性：README 代码片段能否在仅有本模块及已声明依赖的情况下编译通过；import 路径是否真实存在。
15. 配置片段有效性：README 的 yaml / properties 片段缩进层级是否与实际配置前缀匹配。

### D. 其他描述
16. README 引用的文件路径、类名、文档锚点是否仍然存在。
17. README 标记为"已废弃/计划移除"的内容，代码现状是否已实际移除或仍能使用。
18. 模块职责、性能与线程安全类断言，代码中有无依据；无依据的绝对化措辞改为中性描述或删除。

## 执行流程
1. 完整遍历 {{MODULE_DIR}}/src/main 下所有源码与资源（含 resources/META-INF、*.yml、*.properties），
   先建立"代码事实清单"。
2. 通读 {{README_PATH}}，逐节与代码事实清单比对。
3. 按上述 18 项输出核对结论：无差异项也必须明确标注"一致"，不得省略。
4. 对每个差异给出处理结论：修正 README / 删除 README 过时内容 / 存疑待人工确认。
5. 仅修改 {{README_PATH}}，最小必要改动。

## 输出要求（先出清单，再改文档）
先输出如下表格再执行修改：

| # | 核对项 | README 原文位置 | README 描述 | 代码实际 | 结论 |
|---|--------|-----------------|-------------|----------|------|

修改完成后输出：
- 「已修改」清单：逐条说明改了什么、依据哪个源文件的哪段代码（给出文件路径与关键行）。
- 「存疑项」清单：代码无法明确判断之处（语义歧义、跨模块行为等），列出并说明为何不自行臆断。
- 若 18 项全部一致，明确输出"本模块 18 项核对均一致，README 无需修改"，不作任何改写。

修订 README 时遵守：
- 保留原有章节结构、标题层级、表格风格与语气，不要推倒重写，不要扩充无关内容。
- 配置项表格维持「配置项 | 说明 | 默认值」三列格式。
- 不新增未经代码验证的能力描述；不为凑完整性编造默认值或使用方式。
- 概述性表述无明确错误的，不要改动。

## 硬性约束
- 事实基准只有源码；源码与 README 冲突即以源码为准。
- 不得修改任何 .java / .xml / .yml 源码文件，本次产物只有 README。
- 不得删除 README 中代码仍然存在的能力描述。
- 不要输出"已全部核对完成"之类的结论性套话，用具体清单证明核对过程。
```

---

## 变量替换示例

| 占位符 | 示例值 |
|--------|--------|
| `{{MODULE_DIR}}` | `cache` |
| `{{README_PATH}}` | `cache/README.md` |

替换后示例开头：

```
## 本次范围（严格限定）
- 组件目录：cache
- 待核对/修订文档：cache/README.md
```

## 本仓库待处理模块

`ai`、`cache`、`common`、`core`、
`data_migration`、`decentralization_discovery`、`dynamic_datasource`、
`jpa_repository`、`monitor`、`mybatis_repository`、
`repository`、`security`、`test`、`threadpool`、
`utils`、`web`

建议按依赖层级自底向上推进（`utils`/`core`/`common` 先做），每完成一个模块提交一次改动，便于回滚与评审。
