# Wymix Framework 设计与兼容边界

基线：Java 17、Spring Boot 3.5.8、Spring Cloud 2025.0.3、MyBatis-Plus 3.5.15。当前 Maven revision 为 3.0.0-SNAPSHOT。

## 设计方向

定位为 Spring MVC 业务系统开发底座。新项目推荐 MyBatis；JPA 已弃用并保留兼容入口，不新增 JPA 功能。框架使用公共查询 AST、服务/控制器契约和变更事件 SPI，运行时能力按依赖组合选择。

## 模块依赖

| 模块 | 内部直接依赖 | 说明 |
|---|---|---|
| utils/threadpool | 无 | 工具/线程池；utils 仍有 Servlet 工具 API |
| common | utils | 返回值、分页、上下文契约 |
| core_api | common | 轻量 BaseService/审计基类/兼容上下文入口 |
| core | common、threadpool、core_api | 可选 Feign、异步、调度、邮件与日志监听器 |
| repository | core_api | 查询 AST、权限和变更事件契约；显式 AOP 依赖 |
| mybatis_repository | repository | 推荐 ORM；单表审计、查询、权限、分页 |
| jpa_repository | repository | 已弃用，仅兼容维护 |
| security/monitor | core_api | 认证/管理端点基础配置 |
| web | core_api | MVC 与 Swagger；显式 AOP，不再传递 security/core |
| dynamic_datasource/data_migration | repository | 路由/事件消费；迁移不再依赖任何 ORM 实现 |
| decentralization_discovery | core | 配置发现、独立集群认证通道 |
| cache | utils | Caffeine 装配 |
| test/ai | 无 | 测试辅助/AI 占位 |

sample 和 code-generator 是辅助项目，不在根聚合构建。CI 编译 sample 的 mybatis/jpa profiles；数据库容器测试需另行运行。

## 安全与配置

- web 不默认启用认证，需要认证显式引入 security。安全模块默认开启；缺少 JWT 密钥给出可读配置错误，过期时间默认 3600 秒。随机密钥仅显式选择，集群必须共享固定密钥。
- 关闭表单登录仅关闭该登录入口，JWT 和授权继续生效。非法、过期、撤销 JWT 返回 401。黑名单按 JWT exp 保留；默认单机存储，可用自定义 JwtTokenBlackList Bean 替换。
- monitor 默认暴露 health/info/prometheus，健康详情 when_authorized。monitor 不再提供 /actuator/** 匿名白名单；安全链只允许匿名 health，其余已暴露端点要求 ROLE_ACTUATOR_ADMIN。没有 security 的应用需自行保护管理通道。
- 集群端点拥有独立链，仅接受 X-Wymix-Cluster-Token；开启发现必须提供至少 32 字节 shared-secret。凭证不授权业务接口和其他管理端点；跨不可信网络应在网关/传输层提供 TLS。
- 广播类型从 EventHandler<T> Bean 注册，客户端 className 必须与注册类型相同；不再远程 Class.forName。
- ENC(...) 是兼容 Base64 编码，**不是加密**；按原 PropertySource 顺序解码，不提高低优先级值的优先级。
- web 默认不修改 JVM 时区；如确需进程级设置，显式配置 wymix.web.jvm-time-zone。JSON 时区应使用 Spring Jackson 配置。

## 查询、路由和审计契约

- 过滤与排序只接受实体映射字段，未知字段、SQL 片段和 @TableField(exist=false) 属性拒绝；尊重 TableId/TableField/TableAlias 声明。客户端不能指定未声明的别名或任意表达式。
- DSL 解析收敛为 repository AST；LIKE_LEFT 为 %value、LIKE_RIGHT 为 value%，两套 ORM 保持一致。输入转换错误明确拒绝。
- CRUD 通过指定基类的 ResolvableType 解析泛型，支持有自己类型参数的中间基类。
- 动态数据源退出内层调用后恢复外层值；切换数据源不代表跨库事务原子性，已获取连接的事务不会自动更换连接。
- MyBatis 审计捕获成功操作，零行、失败、回滚不发布。Spring 事务/显式 session 事务在提交后发布，auto-commit 在执行成功后发布。已审计批处理要求显式事务、autoCommit=false，按 flush 的影响行数处理；后台调用的 Web 元数据为空。
- 已审计表要求明确主键和支持的单表 SQL。复杂 JOIN/LIMIT 修改明确拒绝，插入需要实体主键回填。事件为进程内通知，提交后监听失败无法撤销已经提交的数据库事务，需要持久交付时由业务实现 outbox。
- JPA 不支持手工/批量 DML 审计，迁移见 jpa_repository/JPA_MIGRATION.md。data_migration 仅事件扩展骨架，内置回放尚未实现。

## 运维和维护

线程池区分任务成功、失败、取消、拒绝；周期总数按执行次数统计，未完成按队列与运行数反映。动态扩容先调最大线程数，缩容先调核心线程数；非法设置不污染配置快照。按配置注册的池在容器销毁时关闭并注销。

查询 AST、配置装配、JWT/Actuator/集群权限、H2 审计事务、线程池与路由拥有回归测试。Maven Wrapper 固定构建版本，CI 验证主工程和两套示例编译。JPA 的兼容测试继续保留。

## 尚未保证的能力

WebFlux/Native Image、分布式黑名单、严格单 Leader 共识、跨库事务、分布式幂等和迁移回放均不因引入框架而获得。发现选举没有 term/投票/租约，适用于受控小集群，不提供分区下唯一主保证。ApplicationContextHolder 等兼容静态入口仍有多容器限制。Undertow 仍为 web 的默认容器，选择其他容器须显式调整依赖。
