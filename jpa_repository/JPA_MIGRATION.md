# JPA 弃用与 MyBatis 迁移

`jpa_repository` 进入兼容维护状态，所有公开实现类/接口标注 `@Deprecated(since="3.0", forRemoval=false)`。目前没有承诺删除版本或日期。已有应用可继续使用常规实体 CRUD；新项目统一推荐 MyBatis-Plus 实现。

## 迁移步骤

1. Maven 依赖改为 `org.wymix.framework:mybatis_repository:${revision}`，示例使用默认 `mybatis` profile。
2. 实体基类改为 `org.wymix.framework.repository.mybatis.audit.domain.AbstractAuditEntity`；JPA 的 `@Entity/@Table/@Id/@Column` 替换为 MyBatis 的 `@TableName/@TableId/@TableField`，核对列名、主键生成与逻辑删除策略。
3. DAO 改为 `org.wymix.framework.repository.mybatis.api.BaseRepository`，服务基类改为同包 `AbstractBaseServiceImpl`。公共 BaseService、DevQueryVO、Page 和 Controller API 保持原包名。
4. JPQL、关联加载、级联、乐观锁和事务语义需逐项迁移，不能只做 import 替换。MyBatis 不自动实现 JPA 的关联/级联语义。
5. 需要数据权限时使用 `@EnableDataPermission` 注册注入器；验证每种查询路径，避免仅给 list 方法加权限而遗漏按 ID 读取/修改。
6. 用相同数据集对查询、分页、NULL、LIKE_LEFT/RIGHT、批量操作及审计事件做差分验收。

## 审计边界

JPA 旧 `onPrepareStatement` 路径已失效，相关切面不再自动注册。手工 `@Modifying` 和 `*InBatch` 不提供变更日志，普通 Hibernate 实体事件是另一条能力。显式设置 `wymix.jpa.manual-dml-audit.enabled=true` 会给出启动错误；手动装配旧切面也会明确报不支持，不再静默假装完成日志。需要这些审计能力请迁移 MyBatis。

MyBatis 对已注册的 `@DataChangeLog/@DataMigration` 单表操作采集实际执行结果，事务提交后发布事件；批处理在 flush 后核对影响行数。支持 Spring 事务和显式 MyBatis session commit/rollback。审计表必须有 `@TableId`，插入必须将主键写回实体。复杂 UPDATE/DELETE JOIN、LIMIT 等形状会明确拒绝，需要拆为主键操作。事件是进程内提交后通知，不提供外部消息系统的 exactly-once 或 outbox 保证。
