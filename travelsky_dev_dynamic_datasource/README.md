# travelsky_dev_dynamic_datasource 动态数据源模块

> 在应用启动前（EnvironmentPostProcessor 阶段）注入多数据源，支持运行时按需切换。

## 模块说明

`travelsky_dev_dynamic_datasource` 提供多数据源管理能力。它通过 `EnvironmentPostProcessor` 在 Spring 环境准备阶段即把配置的数据源注入到 `Environment`，随后 `DynamicDatasourceConfiguration` 据此构建多个 `DataSource` 并以 `primary` 指定默认源。相比运行时动态路由，本模块更偏向「启动期确定的多库」场景。

## 功能说明

- **启动期注入** `*.EnvironmentPostProcessor`：在 `application.yml` 解析后、Bean 创建前将数据源属性写入环境，保证后续自动配置能正确识别。
- **多数据源构建** `DynamicDatasourceConfiguration` + `DynamicDataSourceBuilder`：按 `dynamic.datasource.items.<name>` 逐条构建 `DataSource`，`primary` 指定主库。
- **条件装配**：仅当 `dynamic.datasource.enabled=true` 时生效。

## 配置项

配置前缀：`dynamic.datasource`

| 配置项 | 说明 | 默认 |
|--------|------|------|
| `dynamic.datasource.enabled` | 是否启用动态数据源 | false |
| `dynamic.datasource.primary` | 主数据源名称 | — |
| `dynamic.datasource.items.<name>.url` | 数据库连接 URL | — |
| `dynamic.datasource.items.<name>.username` | 用户名 | — |
| `dynamic.datasource.items.<name>.password` | 密码 | — |
| `dynamic.datasource.items.<name>.driverClassName` | JDBC 驱动类 | — |
| `dynamic.datasource.items.<name>.type` | 数据源实现类型（如 HikariCP 全类名） | — |

> ⚠️ 密码以明文写在配置中，生产环境请配合 `security` 模块的 `ENC()` 配置解密能力使用。

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

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_dynamic_datasource</artifactId>
    <version>${revision}</version>
</dependency>
```
