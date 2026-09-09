# travelsky_dev_dynamic_datasource 动态数据源模块

> 在应用启动前（EnvironmentPostProcessor 阶段）注入多数据源，支持运行时按需切换。

## 模块说明

`travelsky_dev_dynamic_datasource` 提供多数据源管理能力。它通过 `DynamicEnvPostProcessor`（`EnvironmentPostProcessor`）在 Spring 环境准备阶段即把配置的数据源注入到 `Environment`，随后 `DynamicDatasourceConfiguration` 据此构建多个 `DataSource` 并以 `primary` 指定默认源。支持注解切换与编程式切换两种方式。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_dynamic_datasource</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

配置前缀：`dynamic.datasource`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `dynamic.datasource.enabled` | 是否启用动态数据源 | `false` |
| `dynamic.datasource.primary` | 主数据源名称（对应 `items` 中的 key） | — |
| `dynamic.datasource.items.<name>.url` | 数据库连接 URL | — |
| `dynamic.datasource.items.<name>.username` | 用户名 | — |
| `dynamic.datasource.items.<name>.password` | 密码 | — |
| `dynamic.datasource.items.<name>.driverClassName` | JDBC 驱动类 | — |
| `dynamic.datasource.items.<name>.type` | 数据源实现类型（如 HikariCP 全类名） | — |

> ⚠️ 密码以明文写在配置中，生产环境请配合 `security` 模块的 `ENC()` 配置解密能力使用。

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `DynamicDatasourceConfiguration` | 按配置构建多 `DataSource`，注册 `DynamicDataSource`（主库为 primary） |
| `DynamicEnvPostProcessor` | 启动期把 `dynamic.datasource.items.*` 写入 Environment（经 `spring.factories` 注册） |
| `DynamicDataSourceAnnotationAdvisor` / `Interceptor` | `@DynamicDatasource` 注解的 AOP 切换支持 |
| `DynamicDataSourceBuilder` | 数据源构建工具 |

## 继承类（业务方可实现）

| 类/接口 | 说明 |
|---------|------|
| 无强制继承 | 直接使用注解或编程式 API 即可 |

## 注解（业务方使用）

| 注解 | 说明 |
|------|------|
| `@DynamicDatasource("slave")` | 类/方法级注解，标注后该方法执行期间切换到指定数据源（`@Inherited`） |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `DynamicDataSourceContextHolder.setDataSource(name)` | 编程式切换数据源；`getDataSource()` 查询当前；`clear()` 清理；`addDataSource(name)` 动态添加 |
| `DynamicDataSourceBuilder` | 代码中自定义构建数据源 |

## 使用示例

```yaml
# application.yml
dynamic:
  datasource:
    enabled: true
    primary: master
    items:
      master:
        url: jdbc:mysql://localhost:3306/master?useSSL=false
        username: root
        password: ENC(xxx)        # 可配合 security 模块解密
        driverClassName: com.mysql.cj.jdbc.Driver
        type: com.zaxxer.hikari.HikariDataSource
      slave:
        url: jdbc:mysql://localhost:3306/slave?useSSL=false
        username: root
        password: root
        driverClassName: com.mysql.cj.jdbc.Driver
        type: com.zaxxer.hikari.HikariDataSource
```

```java
// 1) 注解方式切换
@DynamicDatasource("slave")
public List<Order> listFromSlave() { ... }

// 2) 编程式切换（用完记得 clear）
DynamicDataSourceContextHolder.setDataSource("slave");
try { ... } finally { DynamicDataSourceContextHolder.clear(); }
```
