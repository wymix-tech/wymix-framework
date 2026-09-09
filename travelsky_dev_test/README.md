# travelsky_dev_test 测试模块

> 测试基础设施：Testcontainers 数据库容器封装（MySQL/Oracle/PostgreSQL）与 DBUnit Excel 数据集。

## 模块说明

`travelsky_dev_test` 为集成测试提供可复用的基础设施。`AbstractContainer` 及其子类封装数据库中间件的 Testcontainers 生命周期，让测试在接近生产的环境中进行；DBUnit 相关能力支持以 Excel（xlsx/xls）数据集准备/清理数据库状态，降低集成测试的数据准备成本。

> 运行前提：Testcontainers 需要可用的 Docker 环境。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_test</artifactId>
    <version>${revision}</version>
    <scope>test</scope>
</dependency>
```

> junit-jupiter（5.7.2，本模块显式指定版本，非由 Spring Boot 统一管理）、Testcontainers BOM 1.20.6、DBUnit 2.8.0、POI 4.1.0 随本模块引入。`spring-boot-starter-logging` 已被排除。

## 配置项

本模块无 Spring `@ConfigurationProperties` 绑定，也无专有配置前缀。容器镜像、账号与初始化脚本经 `unittest.properties`（classpath）或系统属性（`-D`，优先）驱动，由 `AbstractContainer` 静态块读取：

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `db.mysql.image` | MySQL 容器镜像 | `mysql:8.4.2` |
| `db.pg.image` | PostgreSQL 容器镜像 | `mysql:8.4.2`（源码现状，疑似复制笔误） |
| `db.oracle.image` | Oracle 容器镜像 | `mysql:8.4.2`（源码现状，疑似复制笔误） |
| `db.username` | 数据库用户名 | `unittest` |
| `db.password` | 数据库密码 | `unit@Test4321` |
| `db.database` | 数据库名 | `test` |
| `init.sql.path` | 初始化 SQL 目录（工作目录相对路径，目录下所有 `.sql` 文件均执行，执行顺序不保证） | `schema/` |
| `docker.client.strategy` | Testcontainers 客户端策略（透传） | — |
| `docker.host` | Docker 连接地址（本模块透传系统属性/`unittest.properties` 至 Testcontainers 配置；`DOCKER_HOST` 环境变量由 Testcontainers 自身读取，非本模块代码） | — |

## 集成类（测试基类清单）

| 类 | 说明 |
|----|------|
| `AbstractContainer` | 基类：加载 `unittest.properties` 并透传 `docker.client.strategy` / `docker.host` 到 Testcontainers 配置 |
| `AbstractDbContainer` | 数据库容器公共逻辑：`@DynamicPropertySource` 自动启动容器、执行 `init.sql.path` 下初始化 SQL，并将连接信息注入 `spring.datasource.druid.*` |
| `AbstractMySqlContainer` / `AbstractOracleContainer` / `AbstractPgSqlContainer` | 数据库容器模板（MySQL / Oracle / PostgreSQL），静态块按属性创建对应容器 |
| `JunitConfiguration` | JUnit 基础配置：`@ExtendWith({SpringExtension.class, DataSetSupportListener.class})` |

## 继承类（业务方可继承/使用）

| 类 | 说明 |
|----|------|
| `@DataSet` | 类/方法级注解声明数据集：`values`（文件路径）、`setup` / `teardown` 操作（默认 `INSERT` / `DELETE`）、`database`（默认 `test`）、`dataSourceNames`（默认 `dataSource`，多数据源时 `values` 数量需与之相等） |
| `XlsxDataSet` / `XlsxTable` / `XlsxDataSetWriter` | Excel 数据集读取/写入：读取时每个 sheet 一张表、首行为列名（`.xls` 走 DBUnit `XlsDataSet`，`.xlsx` 走自定义 POI 实现）；`XlsxDataSetWriter` 复用 DBUnit `XlsDataSetWriter` 流程、仅将工作簿替换为 `XSSFWorkbook`，用于把数据集导出为 `.xlsx` 文件 |
| `Operation` 及实现（`InsertOperation` / `UpdateOperation` / `DeleteOperation` / `RefreshOperation` / `TruncateOperation` / `ClearOperation`） | DBUnit 操作封装 |
| `DataSetSupportListener` | 数据集支撑监听器：测试前按 `@DataSet.setup` 加载数据、测试后按 `teardown` 清理 |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `AbstractContainer` 子类 | 需要新中间件时继承扩展（参照 `AbstractMySqlContainer`：静态块按 `unittest.properties`/系统属性创建容器并赋给 `container` 字段） |
| `AbstractOperation` | 自定义数据集操作 |
| 自定义镜像 / `@Container` | 继承 `AbstractDbContainer` 子类后，可经 `db.*.image` 属性切换镜像版本，或在测试类中另行声明 `@Container` 字段 |

## 使用示例

```java
// 实际用法：继承 AbstractDbContainer 子类，容器自动启动、连接信息自动注入 spring.datasource.druid.*
@SpringBootTest
class EmployeeRepositoryTest extends AbstractMySqlContainer {

    @Test
    @DataSet(values = "dataset/employee.xlsx", setup = Operation.INSERT, teardown = Operation.DELETE)
    void shouldInsertAndQuery() {
        // 使用 Excel 数据集准备数据 + 断言
    }
}
```
