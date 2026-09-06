# TravelSky Dev Framework 总框架设计文档

> 代号 **APD**（TravelSky 企业级 Java 快速开发框架）
> 基于 Spring Boot 3.5.x + Java 17，Maven 多模块架构

本文档从架构视角说明框架的分层、模块职责、依赖约束与关键设计决策（ADR 风格），作为各模块 README 的上层总览。

---

## 1. 设计目标与定位

| 维度 | 说明 |
|------|------|
| 定位 | 面向航信内部业务系统的**快速开发底座**，封装认证、缓存、监控、数据权限、查询 DSL 等通用能力 |
| 目标用户 | 业务线后端团队，期望「引入模块即获得能力」，聚焦业务逻辑 |
| 技术基线 | Spring Boot 3.5.8、Java 17、Log4j2（替代 Logback）、Undertow（替代 Tomcat） |
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
│  dynamic_datasource · data_migration                            │
├──────────────────────────────────────────────────────────────┤
│  核心层 (Core)                                                   │
│  core  (启动/异步/Feign/邮件)                                    │
├──────────────────────────────────────────────────────────────┤
│  基础层 (Foundation)                                             │
│  utils · threadpool · common                                     │
└──────────────────────────────────────────────────────────────┘

辅助项目（不属分层）：code-generator（代码生成）、sample（示例应用）
```

**依赖约束（C4 风格的容器关系）**

- `utils` ← 被所有模块依赖（最底层，无外依赖）
- `common` ← `web` / `repository` / `core` 共享的数据契约（ResultVO / DevQueryVO / Page / 安全上下文）
- `threadpool` ← `core` / `web` / 业务异步场景
- `core` ← 应用层与基础设施层（提供启动、异步、Feign 基础）
- `repository` ← `jpa_repository` / `mybatis_repository`（抽象层）
- `jpa_repository` / `mybatis_repository` ← `web` / 业务应用（二选一或并存）

---

## 3. 模块职责速览

| 层 | 模块 | 一句话职责 |
|----|------|-----------|
| 基础 | utils | 通用工具（JSON/Long 精度、雪花 ID、反射、网络/日期） |
| 基础 | threadpool | 命名线程池注册中心 + 运行时可观测 + TTL 透传 |
| 基础 | common | 统一返回/分页/审计 VO/安全上下文/异常（跨层契约） |
| 核心 | core | 启动引导、异步调度、Feign 动态实例、邮件 |
| 数据 | repository | ORM 无关抽象：变更日志、迁移、查询符号 |
| 数据 | jpa_repository | JPA 实现：审计填充、变更日志、Spec DSL |
| 数据 | mybatis_repository | MyBatis-Plus 实现：查询 DSL、数据权限、分页 |
| 数据 | dynamic_datasource | 启动期多数据源注入 |
| 数据 | data_migration | 变更事件→迁移（骨架） |
| 设施 | security | JWT/表单认证、令牌黑名单、配置解密、BCrypt |
| 设施 | cache | Hazelcast 分布式 + Caffeine 本地多级缓存 |
| 设施 | monitor | Actuator + Micrometer 指标暴露 |
| 设施 | decentralization_discovery | JmDNS 去中心化服务发现 |
| 应用 | web | 统一响应、TraceId、Long 精度、i18n、慢请求、CRUD 基类、Swagger |
| 应用 | test | Testcontainers + DBUnit 测试基础设施 |
| 应用 | ai | AI 能力预留（空壳） |
| 辅助 | code-generator | 表→全栈代码生成（Groovy 脚本） |
| 辅助 | sample | 端到端可运行示例/脚手架 |

---

## 4. 关键设计决策（ADR）

### ADR-001：双 ORM 并存（JPA + MyBatis-Plus）

- **背景**：不同业务场景对 ORM 偏好不同，且部分存量系统使用 MyBatis。
- **决策**：抽象出 `repository` 层（变更日志/迁移/查询符号），`jpa_repository` 与 `mybatis_repository` 各自实现，共享 `common` 的 `DevQueryVO` 查询契约。
- **后果**：业务可二选一或并存；代价是两个实现需各自维护，且 `@DataChangeLog` 等注解存在重复定义。
- **演进（查询 DSL 解析器统一上提）**：filter 词法/语法解析收敛到 `repository` 抽象层（`FilterStringParser` 词法 + `FilterParser` 语法归一，产出 `FilterNode` 统一 AST）。JPA 的 `SpecificationHelper` 与 MyBatis 的 `QueryParseHelper` 均改为「AST 翻译器」，各自只做 AST → Specification / QueryWrapper，行为一致且新增操作符只需在翻译层各加一个分支。

### ADR-002：统一查询 DSL（DevQueryVO + 类 Lisp filter）

- **背景**：前后端对查询条件的约定长期不一致，难以低代码化。
- **决策**：以 `[[field, op, value], AND, [field, op, value]]` 的类 Lisp 嵌套表达式作为统一查询契约，前后端共用，并附 AI 提示词模板。
- **后果**：前端拼条件、后端解析一致；可对接 AI 把自然语言转 filter。代价是表达式需严格成对括号，调试成本略高（见 `FILTER_USAGE_GUIDE.md`）。

### ADR-003：去中心化服务发现（JmDNS）

- **背景**：中小规模内网系统不希望引入 Eureka/Nacos 等注册中心运维负担。
- **决策**：以 JmDNS（mDNS）实现 `_<namespace>._tcp.local.` 的服务发现，条件装配。
- **后果**：零外部依赖即可服务发现；代价是 mDNS 多播在容器/K8s/跨网段环境通常被禁用，适应该边界（见 `decentralization_discovery` README 警告）。

### ADR-004：Long ↔ String 前后端精度防护

- **背景**：JS `Number` 仅 53 位精度，后端 `Long` 主键（雪花 ID）在前端会失真。
- **决策**：序列化端（`utils` 的 `NumberSerializer`）将 Long 转字符串，Web 层配合反序列化回 Long。
- **后果**：彻底规避精度丢失；代价是前端 id 一律为字符串，写入时需框架自动转回。

### ADR-005：无状态 JWT + 配置解密

- **背景**：微服务/前后端分离需要无状态认证，且配置中的密码需保护。
- **决策**：`security` 提供 JWT（含随机密钥/固定密钥/黑名单），配置密码以 `ENC(...)` 包裹自动解密，凭证 BCrypt 加密。
- **后果**：开箱即用的安全能力；代价是 `CustomSecurityConfiguration` 为抽象类需业务继承（上手门槛），且安全自动配置仍用旧式 `spring.factories`。

### ADR-006：条件化自动配置

- **背景**：避免「引入即强制生效」带来的干扰（如不需要缓存也强制建 Hazelcast 实例）。
- **决策**：`cache`、`dynamic_datasource`、`decentralization_discovery`、`web`（swagger）等采用 `@ConditionalOnClass` / `@ConditionalOnProperty` 按需装配。
- **后果**：真正按需加载；但部分自动配置（如 `MyBatisConfiguration`、`MonitorConfiguration`）仍缺少 `@ConditionalOnProperty` 开关，属待补齐项。

---

## 5. 技术栈与版本

| 类别 | 选型 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.5.8、Spring Security 6.x、Spring Data JPA、Spring Cloud OpenFeign 3.1.3 |
| 持久化 | MyBatis-Plus、Hibernate JPA |
| 缓存 | Hazelcast 5.2.5、Caffeine |
| Web 容器 | Undertow（替代 Tomcat） |
| 日志 | Log4j2（替代 Logback） |
| 监控 | Actuator + Micrometer + Prometheus |
| 发现 | JmDNS（mDNS） |
| 测试 | Testcontainers、DBUnit |
| 构建 | Maven（flatten-maven-plugin + `${revision}` 统一版本） |

---

## 6. 已知架构风险与设计债

| 编号 | 风险 | 建议 |
|------|------|------|
| R1 | `ai` 模块为空壳、`data_migration` 方法体为空 | 明确范围，补实现或标注实验性 |
| R2 | 安全/`dynamic_datasource` 用旧式 `spring.factories`，其余用 `AutoConfiguration.imports` | 统一为新式 imports |
| R3 | `CachingConfigurerSupport` 在 Boot 3.x 已废弃 | 改为实现 `CachingConfigurer` |
| R4 | `MyBatisConfiguration`/`MonitorConfiguration` 缺 `@ConditionalOnProperty` | 补全条件装配开关 |
| R5 | Spring Cloud 3.1.3 与 Boot 3.5.8 存在版本落差 | 对齐 Spring Cloud 版本 |
| R6 | `code-generator` 数据库地址/密码硬编码明文 | 改为环境变量/参数 |
| R7 | `@DataChangeLog` 在多个模块重复定义 | 收敛到单一来源 |
| R8 | `RepeatRequestAspect` 部分实现被注释 | 明确是否启用防重复提交 |

详见评估报告 `project-evaluation-report.html`。

---

## 7. 扩展点汇总

- **安全**：继承 `CustomSecurityConfiguration`，注入 `UserDetailsService`；通过 `SecurityCustomizer` 定制过滤链。
- **CRUD**：继承 `AbstractBaseController` / `AbstractBaseServiceImpl`，覆写 `beforeSave`/`afterSave` 等钩子。
- **查询**：扩展 `OperateSymbol` / `FilterSymbolConst` 增加新操作符。
- **线程池**：实现自定义 `RejectedExecutionHandler`，以 `CUSTOM_POLICY` + 类名注入。
- **变更日志**：在 `jpa_repository` 侧用 `@DataChangeLog` + 监听事件落地业务审计。

---

*相关文档：各模块 `README.md`、`QUICK_START.md`、`FILTER_USAGE_GUIDE.md`、`DevQueryVO用法.md`、`project-evaluation-report.html`*
