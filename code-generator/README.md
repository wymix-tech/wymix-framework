# code-generator 代码生成器

> 基于 Groovy 脚本的从数据库表到全栈 CRUD 代码的逆向生成工具。

## 模块说明

`code-generator` 是一个独立的代码生成工具（非运行时依赖），通过 `code-generator.groovy` 读取 MySQL 数据库表结构，自动生成实体（Entity/VO）、Mapper/Repository、Service、Controller 等全栈 CRUD 代码骨架，大幅减少样板代码编写。

## 依赖引入

该模块为开发期工具，建议以 `groovy` 脚本方式独立运行，**不要打包进生产应用**。

## 配置项

生成器在 `code-generator.groovy` 内部通过变量配置输出（非 Spring 配置）：

| 变量 | 说明 |
|------|------|
| 数据库地址 / 端口 / schema | 目标 MySQL 连接信息 |
| 包名前缀 | 生成代码的 `package` 根路径 |
| 输出目录 | 生成文件写入的本地路径 |
| 表名清单 | 需要生成的表 |
| `baseRoute` / `useRouteNameBeClassNamePrefix` | 路由前缀与类名命名方式 |

### 数据库凭证（必须通过环境变量/系统属性注入）

数据库连接信息已不再硬编码，通过环境变量或系统属性注入，缺失时脚本启动即失败：

| 环境变量 | 系统属性 | 说明 |
|------|------|------|
| `GEN_DB_URL` | `gen.db.url` | JDBC 连接串 |
| `GEN_DB_USER` | `gen.db.user` | 数据库用户名 |
| `GEN_DB_PASSWORD` | `gen.db.password` | 数据库密码 |

## 组成（脚本清单）

| 文件 | 说明 |
|------|------|
| `code-generator.groovy` | 主脚本：编排生成流程（Entity/VO/Mapper/Service/Controller） |
| `DatabaseTableFetcher.groovy` | 表结构获取：读取 `information_schema` 表字段与主键 |
| `Tools.groovy` | 工具：驼峰转换、模板渲染等 |

## 生成物与框架约定（继承关系）

生成的代码与框架模块约定一致：

| 生成物 | 继承/依赖 |
|--------|----------|
| `XxxEntity` | 继承 `AbstractAuditEntity`（repository 模块审计基类），`@TableName` 大写表名 |
| `XxxVO` | 继承 `AbstractAuditVO`（common 模块） |
| `XxxMapper` | 继承 MyBatis-Plus `BaseMapper` |
| `XxxService` | 继承 `AbstractBaseServiceImpl`（mybatis_repository） |
| `XxxController` | 继承 `AbstractBaseController`（web 模块），自动暴露标准 CRUD 端点 |

## 使用示例

```bash
# 1) 通过环境变量注入数据库凭证（支持系统属性 gen.db.url / gen.db.user / gen.db.password）
export GEN_DB_URL="jdbc:mysql://127.0.0.1:3306/your_db?useSSL=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8"
export GEN_DB_USER="your_user"
export GEN_DB_PASSWORD="your_password"
# 2) 用 Groovy 运行
groovy code-generator.groovy

# 生成产物示意（以表 employee 为例）
# com.swcares.xxx.entity.EmployeeEntity  (继承 AbstractAuditEntity)
# com.swcares.xxx.vo.EmployeeVO
# com.swcares.xxx.mapper.EmployeeMapper
# com.swcares.xxx.service.EmployeeService
# com.swcares.xxx.web.EmployeeController  (继承 AbstractBaseController)
```
