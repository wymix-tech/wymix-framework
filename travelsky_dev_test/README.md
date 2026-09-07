# travelsky_dev_test 测试模块

> 测试基础设施：Testcontainers 封装（MySQL/Oracle/PostgreSQL/Kafka/Redis）与 DBUnit Excel 数据集。

## 模块说明

`travelsky_dev_test` 为集成测试提供可复用的基础设施。`AbstractContainer` 及其子类封装常见中间件的 Testcontainers 生命周期，让测试在接近生产的环境中进行；DBUnit 相关能力支持以 Excel（xlsx）数据集准备/校验数据库状态，降低集成测试的数据准备成本。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_test</artifactId>
    <version>${revision}</version>
    <scope>test</scope>
</dependency>
```

## 配置项

本模块通过 Testcontainers 标准属性驱动，无专有配置前缀。常见配置：

| 配置项 | 说明 |
|--------|------|
| `spring.test.context.cache.maxSize` | 测试上下文缓存上限 |
| Testcontainers 环境变量（如 `DOCKER_HOST`） | 容器运行环境 |

## 集成类（测试基类清单）

| 类 | 说明 |
|----|------|
| `AbstractContainer` | 容器生命周期统一封装，提供连接信息注入 |
| `AbstractMySqlContainer` / `AbstractOracleContainer` / `AbstractPgSqlContainer` | 数据库容器模板（MySQL / Oracle / PostgreSQL） |
| `AbstractKafkaContainer` / `AbstractRedisContainer` | Kafka / Redis 容器模板 |
| `AbstractDbContainer` | 数据库容器公共逻辑 |
| `JunitConfiguration` | JUnit 基础配置 |

## 继承类（业务方可继承/使用）

| 类 | 说明 |
|----|------|
| `DataSet` / `XlsxDataSet` / `XlsxDataSetWriter` / `XlsxTable` | Excel 数据集定义与写入 |
| `Operation` 及实现（`InsertOperation` / `UpdateOperation` / `DeleteOperation` / `RefreshOperation` / `TruncateOperation` / `ClearOperation`） | DBUnit 操作封装 |
| `DataSetSupportListener` | 数据集支撑监听器 |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `AbstractContainer` 子类 | 需要新中间件时继承扩展 |
| `AbstractOperation` | 自定义数据集操作 |

## 使用示例

```java
@SpringBootTest
@Testcontainers
class EmployeeRepositoryTest {

    @Container
    static PostgreSQLContainer<?> pg = AbstractContainer.postgres();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Test
    void shouldInsertAndQuery() {
        // 使用 Excel 数据集准备数据 + 断言
    }
}
```
