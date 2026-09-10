# TravelSky Dev Framework 总框架设计文档

> 基于 Spring Boot 3.5.8 + Java 17，Maven 多模块架构
> Maven 坐标：`com.travelsky.base:travelsky_dev_parent3:${revision}`，当前 `<revision>` = `3.0.0-SNAPSHOT`

> **事实基准**：本文为各模块 `README.md` 的上层总览；描述冲突时**以模块 README 与源码为准**。

本文档从架构视角说明框架的分层、模块职责、依赖约束与关键设计决策（ADR 风格），作为各模块 README 的上层总览。

---

## 1. 设计目标与定位

| 维度 | 说明 |
|------|------|
| 定位 | 面向企业级业务系统的**快速开发底座**，封装认证、缓存、监控、数据权限、查询 DSL 等通用能力 |
| 目标用户 | 业务线后端团队，期望「引入模块即获得能力」，聚焦业务逻辑 |
| 技术基线 | Spring Boot 3.5.8、Java 17、Log4j2（替代 Logback，仅在 `core` 等引入 starter 的模块中排除 logging）、Undertow（替代 Tomcat，由 `web` 模块引入并排除 Tomcat） |
| 设计原则 | 分层解耦、按需引入、约定优于配置、自动配置条件化 |

---

## 2. 分层架构

框架按职责自下而上分为 **5 层**，依赖方向严格自上而下，禁止反向依赖（基础设施/应用层不得被基础/核心/数据层依赖）。

```
┌──────────────────────────────────────────────────────────────┐
│  应用层 (Application)                                           │
│  web  ·  test  ·  ai(空壳)                                      │
├──────────────────────────────────────────────────────────────┤
│  基础设施层 (Infrastructure)                                     │
│  security · cache · monitor · decentralization_discovery        │
├──────────────────────────────────────────────────────────────┤
│  数据层 (Data)                                                   │
│  repository(抽象) · jpa_repository · mybatis_repository         │
│  dynamic_datasource · data_migration(依赖两套 ORM)               │
├──────────────────────────────────────────────────────────────┤
│  核心层 (Core)                                                   │
│  core  (启动/异步/Feign/邮件)                                    │
├──────────────────────────────────────────────────────────────┤
│  基础层 (Foundation)                                             │
│  utils · threadpool · common                                     │
└──────────────────────────────────────────────────────────────┘

辅助项目（不属分层）：code-generator（代码生成）、sample（示例应用）
```

**依赖约束（POM 实际依赖关系）**

> 下列为各模块 `pom.xml` 中对内部模块的**直接**依赖；未列出的均为传递依赖或第三方依赖。

| 模块 | 直接依赖的内部模块 |
|------|--------------------|
| `utils` | 无（最底层，仅第三方依赖） |
| `threadpool` | 无（自身独立引入 TTL 等第三方依赖） |
| `common` | `utils` |
| `core` | `common`、`threadpool` |
| `repository` | `core` |
| `jpa_repository` / `mybatis_repository` | `repository`（二选一或并存） |
| `security` | `core` |
| `cache` | `utils` |
| `monitor` / `decentralization_discovery` | `core` |
| `dynamic_datasource` | `repository` |
| `data_migration` | `jpa_repository` **与** `mybatis_repository`（两者同时引入，按 classpath 条件导入对应的变更日志装配） |
| `web` | `core`、`security`（因此引入 `web` 会自动带入安全能力，默认开启） |
| `test` | 无内部依赖（`scope=test`，自带 Testcontainers / DBUnit / POI） |

由此可得的约束与注意点：

- `web` → `security` → `core` → `common` → `utils` 是最长的一条传递链，**引入 `web` 几乎等价于引入半个框架**。
- `cache` 刻意只依赖 `utils`，不触碰 `core` / `security`，便于在纯工具场景单独使用。
- 数据层与基础设施层之间无横向依赖（`security` 不经由数据层），跨层访问一律通过 `common` 的数据契约。

---

## 3. 模块职责速览

> 模块开关统一规律：业务相关装配多使用 `@ConditionalOnProperty(matchIfMissing = true)` 的**默认开启**模式，
> 而「会改变全局默认行为」的装配（如 `dynamic_datasource`、`decentralization_discovery`）采用**默认关闭**的 opt-in 模式。

| 层 | 模块 | 一句话职责 | 装配开关（前缀） |
|----|------|-----------|------------------|
| 基础 | utils | 通用工具（JSON/Long 精度、雪花 ID、反射、网络/日期） | 无自动配置（纯工具类） |
| 基础 | threadpool | 命名线程池注册中心 + 运行时可观测 + TTL 透传 | `travelsky.threadpool.enabled`(默认 true) |
| 基础 | common | 统一返回/分页/审计 VO/安全上下文/异常（跨层契约） | 无自动配置（数据契约） |
| 核心 | core | 启动引导与监听器、异步调度、Feign 动态实例、邮件/消息门面 | `travelsky.core.enabled`、`async.enabled`(默认 true) |
| 数据 | repository | ORM 无关抽象：变更日志、迁移注解、filter DSL 解析器（词法+语法→AST） | 无自动配置（抽象契约） |
| 数据 | jpa_repository | JPA 实现：审计填充、变更日志、Spec DSL 翻译 | `travelsky.jpa.enabled`、`data-changelog.enabled`(默认 true) |
| 数据 | mybatis_repository | MyBatis-Plus 实现：查询 DSL 翻译、数据权限、分页 | `travelsky.mybatis.enabled`、`data-changelog.enabled`(默认 true) |
| 数据 | dynamic_datasource | 环境准备期多数据源注入 + 注解/编程式路由 | `dynamic.datasource.enabled`(**默认 false**) |
| 数据 | data_migration | 变更事件→迁移回调（迁移回放未实现） | `travelsky.data-migration.enabled`(默认 true) |
| 设施 | security | JWT/表单认证、令牌黑名单、配置解密、BCrypt | `security.enabled`(默认 true) |
| 设施 | cache | Caffeine 本地缓存装配（`@ConditionalOnClass(Caffeine)`，`caffeine` 为 optional 依赖） | `cache.enabled`(默认 true) |
| 设施 | monitor | Actuator 默认值注入 + `/actuator/**` 放行 | `travelsky.monitor.enabled`(默认 true) |
| 设施 | decentralization_discovery | JmDNS 去中心化服务发现（JmDNS 为 optional 依赖） | `decentralization.discovery.enable`(**默认 false**) |
| 应用 | web | 统一响应、TraceId、Long 精度、i18n、慢请求、CRUD 基类、Swagger | `swagger.enabled`(默认 true)、`swagger.web.enabled`(默认 false) |
| 应用 | test | Testcontainers + DBUnit 测试基础设施 | 测试域，无装配开关 |
| 应用 | ai | AI 能力预留（**空壳模块**，仅坐标 + README） | 无自动配置 |
| 辅助 | code-generator | 表→全栈代码生成（Groovy 脚本） | 开发期工具，非运行时依赖 |
| 辅助 | sample | 端到端可运行示例/脚手架 | `sample` 自身不发布为依赖 |

## 4. 关键设计决策（ADR）

### ADR-001：双 ORM 并存（JPA + MyBatis-Plus）

- **背景**：不同业务场景对 ORM 偏好不同，且部分存量系统使用 MyBatis。
- **决策**：抽象出 `repository` 层（变更日志/迁移/查询符号），`jpa_repository` 与 `mybatis_repository` 各自实现，共享 `common` 的 `DevQueryVO` 查询契约。
- **后果**：业务可二选一或并存；代价是两个实现需各自维护（翻译层、审计、分页各有一套）。
- **演进（`621bf37`，已完成）**：filter 词法/语法解析已收敛到 `repository` 抽象层
  （`FilterStringParser` 词法 + `FilterParser` 语法归一，产出 `ConditionNode` / `LogicalNode` / `GroupNode` 组成的 `FilterNode` AST）。
  JPA 的 `SpecificationHelper` 与 MyBatis 的 `QueryParseHelper` 均降级为「AST 翻译器」，各自只做
  AST → `Specification` / `QueryWrapper`，新增操作符只需在两个翻译层各加一个分支。
- **现状补充**：`@DataChangeLog` / `@DataMigration` 注解已单一来源定义在 `repository`
  （`com.travelsky.base.repository.datachangelog.annotation` / `...datamigration.annotations`），
  历史重复定义问题已消解。两套 ORM 的**业务基类包路径已隔离**（`...repository.mybatis.*` 与 `...repository.jpa.*`），可并存引入。
- **能力差异（非对称）**：数据权限（`@DataPermission` / `PermissionSqlInjector`）**仅 MyBatis 侧提供**，JPA 侧无对应实现。

### ADR-002：统一查询 DSL（DevQueryVO + 类 Lisp filter）

- **背景**：前后端对查询条件的约定长期不一致，难以低代码化。
- **决策**：以 `[[field, op, value], AND, [field, op, value]]` 的类 Lisp 嵌套表达式作为统一查询契约，前后端共用，并附 AI 提示词模板。
- **传输约定**：`DevQueryVO` 无 `@RequestBody`，由 MVC 按平铺参数绑定，因此 `filter` / `sorts` 均以**字符串**形式传输
  （`sorts` 是 JSON 数组的字符串表示），不是嵌套 JSON 对象。
- **语义归一（解析器层实现）**：逻辑符大小写不敏感；同级混合时 **AND 优先**（`A OR B AND C` → `A OR (B AND C)`）；
  多个无条件连接的条件按 AND 合并；`in`/`between` 的值支持 `[f,in,[a,b]]` 与 `[f,in,a,b]` 两种等价写法。
- **后果**：前端拼条件、后端解析一致；可对接 AI 把自然语言转 filter。代价是表达式需严格成对括号，调试成本略高（见 `FILTER_USAGE_GUIDE.md`）。

### ADR-003：去中心化服务发现（JmDNS）

- **背景**：中小规模内网系统不希望引入 Eureka/Nacos 等注册中心运维负担。
- **决策**：以 JmDNS（mDNS）实现 `_<namespace>._tcp.local.` 的服务发现，`decentralization.discovery.enable` opt-in，JmDNS 依赖 `optional`。
- **后果**：零外部依赖即可服务发现；代价是 mDNS 多播在容器/K8s/跨网段环境通常被禁用（见 `decentralization_discovery` README 警告）。
- **当前阻塞**：本模块 `NodeService` 依赖 Spring Cloud OpenFeign 的 `FeignContext`，而父 POM 锁定 `spring-cloud-starter-openfeign:3.1.3`
  （2021.x，仅 Boot 2 时代的 `spring.factories` 装配）。在 Boot 3.5 下开启该模块会报 `FeignContext` Bean 缺失，
  需先对齐 Spring Cloud 版本（风险 R5）。

### ADR-004：Long ↔ String 前后端精度防护

- **背景**：JS `Number` 仅 53 位精度，后端 `Long` 主键（雪花 ID）在前端会失真。
- **决策**：序列化端（`utils` 的 `NumberSerializer`）在**数值 > `100000000000000`（1e14）时输出字符串**，未超阈值仍按数字输出；
  `web` 的 `WebConfiguration` 在 Jackson `ObjectMapper` 上注册该序列化器（作用于 `Long` 与 `Number`）。
- **后果**：兼顾精度与可读性——雪花 ID（通常超阈值）以字符串到达前端规避精度丢失，普通小数值保持数字类型。
  代价是**框架未提供配套的 String→Long 反序列化器**，写入实体 ID 时的类型转换需业务侧处理；前端也不应假设「所有整型都是字符串」。

### ADR-005：无状态 JWT + 配置解密

- **背景**：微服务/前后端分离需要无状态认证，且配置中的密码需保护。
- **决策**：`security` 提供 JWT（随机/固定密钥、黑名单），配置密码以 `ENC(...)` 包裹自动解密，凭证 BCrypt 加密。
- **装配方式**：**已无需继承**。自动装配类 `CustomSecurityConfiguration`（`@AutoConfiguration`）默认生效（`security.enabled` `matchIfMissing = true`），
  业务方只需声明 `UserDetailsService` Bean 提供用户来源，并可用 `SecurityCustomizer` 函数式扩展 `HttpSecurity`。
- **后果**：真正的开箱即用；代价是①黑名单 `JwtTokenBlackList` 为**单机内存态**，集群下登出不互通；
  ②`web` 模块 compile 依赖本模块，**仅引入 `web` 也会默认开启安全**，不需要认证时需显式 `security.enabled: false`。
- **注册机制说明**：安全自动配置本身已迁入新式 `AutoConfiguration.imports`；`spring.factories` 中仅保留 `EnvironmentPostProcessor`
  （`DecryptEnvironmentPostProcessor`）——该类必须先于自动装配阶段执行，这是 Spring Boot 3 的唯一注册方式，**不属于技术债**（见 §6 中 R2 说明）。

### ADR-006：条件化自动配置

- **背景**：避免「引入即强制生效」带来的干扰（如不需要缓存也强制建缓存实例）。
- **决策**：`cache`、`dynamic_datasource`、`decentralization_discovery`、`web`（swagger）等采用 `@ConditionalOnClass` / `@ConditionalOnProperty` 按需装配。
- **现状（R4 已修复）**：`MyBatisConfiguration`（`travelsky.mybatis.enabled`）与 `MonitorConfiguration`（`travelsky.monitor.enabled`）
  **均已具备 `@ConditionalOnProperty(matchIfMissing = true)` 开关**，各主要装配的条件开关已补齐。
- **后果**：真正按需加载；仍存的差异是 `dynamic_datasource` / `decentralization_discovery` 为 opt-in（默认关闭），
  其余业务能力默认开启——引入依赖前建议先确认该模块的开关前缀（见 §3 表格）。

---

## 5. 技术栈与版本

| 类别 | 选型 | 版本 / 位置 |
|------|------|-------------|
| 语言 | Java 17 | `<java.version>17` |
| 框架 | Spring Boot（父 POM） | `spring-boot-starter-parent:3.5.8` |
| 框架 | Spring Security | 6.x（随 Boot 3.5.8 管理） |
| 框架 | Spring Data JPA | 随 Boot 管理 |
| 框架 | Spring Cloud OpenFeign / LoadBalancer | **3.1.3**（2021.x，与 Boot 3.5 版本落差，见 R5） |
| 持久化 | MyBatis-Plus | `mybatis-plus-spring-boot3-starter` + `mybatis-plus-jsqlparser`（版本由父 POM import 的 `mybatis-plus-bom:3.5.15` 管理） |
| 持久化 | Hibernate JPA | 随 Spring Data JPA |
| 缓存 | Caffeine（`optional`，需业务方显式引入） | 未显式指定，由 `spring-boot-dependencies` 管理 |
| Web 容器 | Undertow（`web` 模块排除 Tomcat 后引入） | 随 Boot 管理 |
| 日志 | Log4j2（`core` / `web` / `test` 等模块各自排除 `spring-boot-starter-logging` 后引入 `spring-boot-starter-log4j2`） | 随 Boot 管理 |
| 监控 | Actuator + Micrometer + Prometheus | 随 Boot 管理 |
| 发现 | JmDNS（mDNS，`optional`） | 3.5.12 |
| 安全 | JJWT | 0.13.0（`<jjwt.version>`） |
| 测试 | Testcontainers 1.20.6、DBUnit 2.8.0、POI 4.1.0、JUnit 5.7.2 | `travelsky_dev_test`（test scope） |
| 构建 | Maven（`flatten-maven-plugin` + `${revision}` 统一版本） | `<revision>3.0.0-SNAPSHOT</revision>` |

---

## 6. 已知架构风险与设计债

> 状态说明：R3 / R4 / R7 已在近期提交中修复，保留条目仅作变更轨迹。

| 编号 | 风险 | 状态 | 建议 |
|------|------|------|------|
| R1 | `ai` 模块为空壳（仅 `pom.xml` + README）；`data_migration` 迁移回放未实现 | **已标注（实验性）** | 均定位为实验性/预留模块：`ai` README 已声明「勿在生产直接依赖」；`data_migration` 回放未实现时输出 `warn` 级别日志提示 |
| R2 | 安全 / `dynamic_datasource` / `core` / `monitor` 仍用 `spring.factories`，其余用 `AutoConfiguration.imports` | **部分不成立** | 上述 `spring.factories` 中注册的均为 `EnvironmentPostProcessor` / `ApplicationListener`（须先于自动装配与环境准备阶段执行），Boot 3 无替代注册方式，**属设计约束而非技术债**；真正的自动配置类已全部迁入 `AutoConfiguration.imports` |
| R3 | `CachingConfigurerSupport` 在 Boot 3.x 已废弃 | **已修复** | `CacheManagerConfiguration.CacheCachingConfigurer` 已 `implements CachingConfigurer`（`b003f92` 起） |
| R4 | `MyBatisConfiguration` / `MonitorConfiguration` 缺 `@ConditionalOnProperty` | **已修复** | 已分别添加 `travelsky.mybatis.enabled` / `travelsky.monitor.enabled`（`matchIfMissing = true`） |
| R5 | Spring Cloud 3.1.3 与 Boot 3.5.8 存在版本落差 | **未解决（阻塞性）** | 对齐 Spring Cloud 到 2025.x；在此之前 `decentralization_discovery` 不可用（`FeignContext` 缺失），Feign 相关能力亦受影响 |
| R6 | `code-generator` 硬编码 | **已修复** | 数据库凭证、包名（`GEN_BASE_PACKAGE`）、输出目录（`GEN_OUTPUT_DIR`）、类名前缀开关（`GEN_ROUTE_AS_CLASS_PREFIX`）均改为环境变量 / 系统属性注入，缺失时快速失败；Windows 路径硬编码已移除 |
| R7 | `@DataChangeLog` 在多个模块重复定义 | **已修复** | 单一来源：`travelsky_dev_repository` 的 `...datachangelog.annotation.DataChangeLog` |
| R8 | `@RepeatRequest` 默认不生效 | **已缓解（文档 + 开关）** | `web` 模块默认不注册切面（去重粒度需业务确认），README 已显著标注；新增 `travelsky.web.repeat-request.enabled=true` 开关注册内置切面，业务方自声明 Bean 仍优先（`@ConditionalOnMissingBean`） |
| R9 | 数据权限仅 MyBatis 侧实现 | **已修复** | 数据权限上下文/注解/切面上提到 `travelsky_dev_repository`；JPA 侧由 `SpecificationHelper#applyDataPermission` 将 `WHERE_ADDITIONAL` 注入器翻译为 `Predicate` 追加（`REPLACE_TABLE_NAME` 跳过、`statementIds` 不支持按语句过滤会告警、字段名列名/属性名双向映射）；注入器获取与 injectSql 解析抽为 `DataPermissionSupport`，两侧共用且未注册为 Bean 时有明确异常
| R10 | JWT 黑名单为单机内存态 | **未解决** | 集群部署需替换为 Redis 等共享存储的黑名单实现 |
| R11 | MyBatis 分页 SQL 注入改写依赖 JSqlParser，`@DynamicDatasource` 注解查找存在实现缺陷 | **已修复** | 回退判空由 `Optional != null` 改为 `Optional.isPresent()`；Mapper 分支原取 `clazz.getSuperclass()`（接口恒为 `null`，该回退是死代码）改为遍历 `getInterfaces()`；`getSuperclass()` 判空；单测覆盖类继承回退、方法重写回退与 Mapper（父）接口回退 |

> ⚠️ 历史评估报告 `project-evaluation-report.html` **已不在仓库中**（`e7d1dea` 移除），
> 上述风险条目以本文档 §3 表格与各模块 README 的现状描述为准。

---

## 7. 扩展点汇总

- **安全**：声明 `UserDetailsService` Bean 提供用户来源（无需继承任何配置类）；声明 `SecurityCustomizer` Bean 定制过滤链；覆盖 `JwtTokenBlackList` / 各类处理器 Bean（`AuthenticationEntryPoint` 等均 `@ConditionalOnMissingBean`）。
- **CRUD**：继承 `AbstractBaseController` / `AbstractBaseServiceImpl`，覆写 `beforeSave` / `afterSave` / `beforeUpdate(old, cur)` / `afterUpdate` 钩子；覆写 `mappedToVO` / `mappedToPO` 替换默认 VO↔PO 反射转换。
- **查询**：扩展 `OperateSymbol`（`common`）增加操作符后，在 `QueryParseHelper#addCondition`（MyBatis）与 `SpecificationHelper#buildPredicate`（JPA）各补一个分支——解析层（`FilterParser`）无需改动。
- **线程池**：实现 `ThreadPoolExecutor.RejectedExecutionHandler`，以 `CUSTOM_POLICY` + `rejectedPolicyClassName` 注入；运行时用 `ThreadPoolMonitor` / `ThreadPoolOperator` 观测与调参。
- **缓存**：覆盖名为 `keyGenerator` 的 Bean 或名为 `cacheManager` 的 Bean；如需完全自管请先 `cache.enabled=false`（模块固定注册 `CacheCachingConfigurer`，与业务 `CachingConfigurer` 二选一）。
- **变更日志 / 迁移**：声明自定义 `DataChangeLogEventListener` Bean 覆写 `changelog(DataChangeLogDTO)`；或直接用 `@EventListener(DataChangeLogEvent)` 自行处理。
- **数据权限（仅 MyBatis）**：实现 `PermissionSqlInjector`（`injectSql()` / `additionalParameters()` / `type()`），用 `@EnableDataPermission(basePackages=...)` 扫描，再以 `@DataPermission(injectors = {...})` 标注方法。
- **消息通道**：实现 `MessageSenderService` SPI 并注册 Bean，`MessageService` 按 `SendType`（`O` / `E` / `P`）分发；注意 `E`、`P` 在本仓库内无实现。
- **i18n**：通过 `IgnoreUrlContext` Bean 追加免认证路径；通过 `messages*` / `i18n/validation/ValidationMessages*` 资源扩展文案。
- **启动期扩展**：`DecryptEnvironmentPostProcessor`（配置解密）与 `LogApplicationStartedEventListener` / `FeignApplicationStartedEventListener` / `MonitorApplicationStartedEventListener` 均经 `spring.factories` 注册，业务方可用原生 `SpringApplication.run` 启动。

---

*相关文档：各模块 `README.md`、`QUICK_START.md`、`FILTER_USAGE_GUIDE.md`、`travelsky_dev_mybatis_repository/DevQueryVO用法.md`、根目录 `README.md`（模块导航）*
