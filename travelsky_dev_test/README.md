# travelsky_dev_test 测试模块

> 测试基础设施：Testcontainers 封装（Oracle/PostgreSQL/Kafka/Redis）与 DBUnit Excel 数据集。

## 模块说明

`travelsky_dev_test` 为集成测试提供可复用的基础设施。`AbstractContainer` 封装常见中间件的 Testcontainers 生命周期（Oracle、PostgreSQL、Kafka、Redis 等），让测试在接近生产的环境中进行；DBUnit 相关能力支持以 Excel 数据集准备/校验数据库状态，降低集成测试的数据准备成本。

## 功能说明

- **容器封装** `AbstractContainer`：统一拉起/销毁中间件容器，提供连接信息注入。
- **多中间件支持**：Oracle / PostgreSQL / Kafka / Redis 等容器模板。
- **数据集驱动**：DBUnit 以 Excel 作为数据准备与断言的数据源。

## 配置项

本模块通过 Testcontainers 标准属性驱动，无专有配置前缀。常见配置：

| 配置项 | 说明 |
|--------|------|
| `spring.test.context.cache.maxSize` | 测试上下文缓存上限 |
| Testcontainers 环境变量（如 `DOCKER_HOST`） | 容器运行环境 |

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

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_test</artifactId>
    <version>${revision}</version>
    <scope>test</scope>
</dependency>
```
