# travelsky_dev_mybatis_repository MyBatis 数据访问模块

> 基于 MyBatis-Plus 的持久化实现：分页插件、数据权限 AOP 与统一查询 DSL（`DevQueryVO`）。

## 模块说明

`travelsky_dev_mybatis_repository` 在 MyBatis-Plus 之上提供企业级数据访问能力。核心是统一的查询 DSL `DevQueryVO`：前端/调用方提交一段类 Lisp 的嵌套括号表达式（filter），后端自动解析为 SQL 条件。同时提供 `@DataPermission` 数据权限 AOP 与分页插件。

详细查询语法见同目录文档：
- [DevQueryVO用法.md](./DevQueryVO用法.md)（后端视角）
- 根目录 `FILTER_USAGE_GUIDE.md`（前端视角，含 AI 提示词模板）

## 功能说明

| 能力 | 关键类 | 说明 |
|------|--------|------|
| 查询 DSL | `DevQueryVO` + 解析器 | 类 Lisp 嵌套表达式 `[[field, op, value], AND, [field, op, value]]` → SQL |
| 分页插件 | `MyBatisConfiguration` | 自动注册 MyBatis-Plus 分页插件 |
| 数据权限 | `@DataPermission` + `DataPermissionAspect` | 方法级数据权限拦截，按部门/角色过滤数据 |
| 审计基类 | `AbstractAuditEntity`（来自 `repository`） | 实体继承后维护审计字段 |

## 配置项

本模块通过 MyBatis-Plus 标准配置（`mybatis-plus.*`）驱动，无额外专有前缀。`@DataPermission` 为注解驱动，无需配置开关。

## 使用示例

```java
// 1) Mapper 继承 MyBatis-Plus 基类
@Mapper
public interface EmployeeMapper extends BaseMapper<EmployeeEntity> {}

// 2) 实体继承审计基类
@Data
@TableName("EMPLOYEE")
public class EmployeeEntity extends AbstractAuditEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Integer age;
}

// 3) 数据权限（仅返回当前用户可见部门数据）
@DataPermission(dept = true)
public List<EmployeeEntity> listByDept() {
    return employeeMapper.selectList(null);
}

// 4) 前端提交的查询 JSON
// { "filter": "[[name, CONTAINS, 张], AND, [age, >=, 18]]",
//   "sorts": "[{\"columns\":[\"createTime\"],\"direction\":\"DESC\"}]",
//   "page": 1, "limit": 20 }
```


> **包路径说明（3.x 起）**：MyBatis-Plus 专属类已迁移到 `com.travelsky.base.repository.mybatis.*` 子包，
> 与 JPA 实现完全隔离，两模块可并存引入。旧包路径 `com.travelsky.base.repository.api` /
> `...audit.domain` 保留 deprecated 兼容别名（`forRemoval = true`），请尽快迁移。
> 示例中的 import：
>
> ```java
> import com.travelsky.base.repository.mybatis.api.BaseRepository;
> import com.travelsky.base.repository.mybatis.api.AbstractBaseServiceImpl;
> import com.travelsky.base.repository.mybatis.audit.domain.AbstractAuditEntity;
> ```

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_mybatis_repository</artifactId>
    <version>${revision}</version>
</dependency>
```
