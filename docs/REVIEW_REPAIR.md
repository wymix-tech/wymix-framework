# 评审修复与接入变化（2026-10-14）

本次对应评审 F01–F18。JPA 采用“弃用并保留兼容”的方案，新项目推荐 MyBatis。具体迁移见 [JPA_MIGRATION.md](../jpa_repository/JPA_MIGRATION.md)。

## 必须调整的配置

- 使用认证的应用显式引入 security；web 不再传递它。配置 security.jwt.secret（至少 32 UTF-8 字节）；expired-time 默认 3600 秒，单位始终为秒。
- 关闭 form-login-enable 后，受保护请求仍需 JWT。无效、过期、撤销令牌返回 401。登出黑名单覆盖签名 exp 的剩余寿命。
- monitor 不再放行 /actuator/**。默认只暴露 health/info/prometheus；已启用框架安全时 info/prometheus 需 ROLE_ACTUATOR_ADMIN。
- 服务发现默认关闭；开启时必须配置 decentralization.discovery.shared-secret，并在各节点保持一致。集群内部调用自动携带 X-Wymix-Cluster-Token，外部调用也必须提供该凭证；它不授权普通业务 URL 或其他管理端点。
- 查询 filter/sorts 只能使用实体持久化属性。列名自定义由 @TableField/@TableId 配置，别名须 @TableAlias 声明，不允许 SQL 表达式或未知字段。
- 不再默认改变 JVM 时区；需要保留旧进程行为时显式设置 wymix.web.jvm-time-zone: GMT+08:00。推荐只配置 spring.jackson.time-zone。
- ENC(...) 只是 Base64 编码，不能保护密码；解码保持 PropertySource 优先级。生产使用 Secret 管理或受支持的加密配置组件。
- 需要框架 Feign、异步/调度、邮件和日志监听器时另行引入 core。repository/security/monitor/web 只依赖 core_api，保留原公共 API 包名。
- data_migration 不再带入 ORM；分别显式引入事件生产者。其内置迁移回放仍未实现。

## 修复对应表

| 评审编号 | 改动 | 回归方式 |
|---|---|---|
| F01 | 收紧监控暴露与授权 | 默认配置、匿名/用户/管理员 HTTP 测试 |
| F02 | 过滤/排序字段白名单与映射 | SQL 片段、未知/虚拟字段、类型错误、映射列测试 |
| F03 | JWT/授权独立于表单开关 | 无表单登录的 MockMvc 鉴权与扩展测试 |
| F04/F05/F18 | exp 黑名单、单次解析、受控错误和默认配置校验 | JWT/登出/缺失配置测试 |
| F06 | 嵌套路由恢复 | 成功与异常退出测试 |
| F07 | 下线失效 JPA 手工 DML 审计并明确弃用 | 不再注册旧切面，显式启用会快速失败 |
| F08/F09 | 执行成功并在提交后发布 MyBatis 事件 | H2 的生成 ID、NULL、删除、后台、零行、失败、回滚、batch/auto-commit 测试 |
| F10 | 节点/负载均衡统一注销与清空 | 生命周期回归测试 |
| F11/F12 | 任务结果统计与有序调参 | future 失败、周期、取消、拒绝、扩缩容测试 |
| F13 | 保留配置来源优先级 | 编码/明文混合来源测试 |
| F14/F15 | 标准 Map 契约与正负整数精度 | null computation、JSON 边界/NaN 测试 |
| F16/F17 | LIKE 语义统一、指定基类泛型解析 | 两侧代码语义一致、多层基类测试 |

## 审计注意事项

MyBatis 已审计表必须标注 @TableId；插入需通过实体参数回填主键。支持单表 DML，复杂 UPDATE/DELETE JOIN/LIMIT 明确拒绝。已审计 batch 必须使用明确事务（连接 autoCommit=false），自动提交 batch 在写入排队前拒绝。标准 batch 在 flush 后验证影响行数；重复修改同一主键的 batch 记录反映 flush 前后状态，不保证每个中间数据库状态。框架自动装配保证权限插件作为外层先改写 SQL，审计消费已改写语句；手工组装 MyBatis 时，应先注册 DataChangeLogPlugin、后注册 DataPermissionPlugin。事件为提交后的本进程通知，不等同于持久消息队列；提交后消费者失败不会回滚数据库，需要可靠外发时应使用 outbox。

Caffeine 自定义权重函数允许正常业务包，初始化前验证 Weigher 类型，不再用框架包名前缀限制扩展。

## 保留的边界

JPA 常规 CRUD 与有限权限继续兼容；不扩建其手工审计。JWT 黑名单仍为默认单机实现，可用自定义 Bean 替换。发现组件不是共识算法；RepeatRequest 是本机会话内节流，不是分布式幂等。全量 CVE 扫描、性能压测与生产网络分区验证不属于本次已验证结论。

## 构建验证

`./mvnw clean verify` 验证主工程；测试 Agent 在 process-test-classes 阶段准备，Mockito 不需要 JVM 自附加。额外 JVM 参数使用 `-Dtest.jvm.args=...`。

示例独立构建：先 `./mvnw -DskipTests install`，再 `./mvnw -f sample/pom.xml -Pmybatis -DskipTests package` 与 `./mvnw -f sample/pom.xml -Pjpa -DskipTests package`。示例启动前设置 JWT_SECRET；启用发现时另设 DISCOVERY_SHARED_SECRET。数据库容器测试须有 Docker，并按自身配置另外运行。

## 本次验证结果

Java 17 + Maven 3.9.11 主聚合工程构建成功，Maven 各模块汇总 **155 项测试、0 失败、0 错误、0 跳过**。此前报告 XML 顶层计数遗漏嵌套测试，原基线应为 125 项，本次新增 30 项。

MyBatis 和 JPA 两套 sample profile 均编译打包成功（跳过示例数据库容器测试）。Java 21 的验证由新增 CI matrix 执行，本地未声称已经验证。

额外修复 MyBatis 数据权限命名参数的 JDBC 顺序绑定、空值类型和主键回填元数据；不再修改调用方参数对象。新增 H2 用例验证权限过滤和审计使用同一执行范围，并在入队前拒绝无事务的已审计 batch。

## 线程池专项修复（后续）

默认执行器/调度器改由 Boot 3.5 原生装配，保留官方配置、用户扩展、force、生命周期与装饰器。命名业务池通过 BeanDefinition 注册并与默认池候选隔离；修复 DiscardOldest 的新任务误取消和重复包装，提供 submitCompletable 的异常/取消传播。原生池结果统计明确以 outcomeMetricsAvailable=false 标识不可观测，不再误报成功率。迁移与兼容变化见 [THREADPOOL_BOOT35.md](THREADPOOL_BOOT35.md)。本节后续构建结果单独报告，上述 155 项属于上一轮修复。

## Spring Boot 3.5.16 升级

父 POM 从 3.5.8 升级至 3.5.16，聚合模块和 sample 继承同一依赖管理。Spring Framework 由 Boot BOM 管理为 6.2.19；Java 基线保持 17，Spring Cloud 保持匹配 3.5.x 版本线的 2025.0.3。README 与框架设计基线同步更新，历史评审记录保留原版本。
