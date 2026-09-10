# travelsky_dev_dynamic_datasource 动态数据源模块

> 在应用启动前（EnvironmentPostProcessor 阶段）注入多数据源，支持运行时按需切换。

## 模块说明

`travelsky_dev_dynamic_datasource` 提供多数据源管理能力。它通过 `DynamicEnvPostProcessor`（`EnvironmentPostProcessor`）在 Spring 环境准备阶段扫描 `dynamic.datasource.items.*` 配置，为每个数据源补齐 `name` / `jdbcUrl` / `poolName` 属性后写入 `Environment`（`MapPropertySource`，追加在 PropertySource 列表末尾）；随后由 `DynamicDatasourceConfiguration` 据此通过 `DynamicDataSourceBuilder` 构建各 `DataSource`，组装为单个 `DynamicDataSource`（`AbstractRoutingDataSource` 子类）并以 `primary` 指定默认源。支持注解切换与编程式切换两种方式。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_dynamic_datasource</artifactId>
    <version>${revision}</version>
</dependency>
```

> ⚠️ 本模块依赖仅含 `spring-jdbc`（及内部模块 `travelsky_dev_repository`），**不含 JDBC 驱动与连接池实现**，需自行补充：

> - 数据库驱动：按实际使用的数据库引入（如 `mysql-connector-j`）；
> - 连接池：默认 `type` 为 `com.zaxxer.hikari.HikariDataSource`，需自行引入 `com.zaxxer:HikariCP`（可参考 `sample/pom.xml` 通过 profile 显式声明的写法），未引入时构建数据源将抛 `ClassNotFoundException`。

> ⚠️ 模块默认**不启用**（`dynamic.datasource.enabled` 默认为 `false`）。启用需在配置中显式设置 `dynamic.datasource.enabled: true`，并提供 `primary` 与至少一个数据源条目，最小可用配置见下方「使用示例」。

## 配置项

配置前缀：`dynamic.datasource`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `dynamic.datasource.enabled` | 是否启用动态数据源自动装配 | `false`（未配置或非 `true` 时装配类不生效，但 `DynamicEnvPostProcessor` 仍会在环境准备阶段执行注入） |
| `dynamic.datasource.primary` | 主数据源名称（对应 `items` 中的 key）；未配置时装配阶段 `getPrimary()` 返回 `null`，构建 `DataSource` Bean 时抛 NPE | — |
| `dynamic.datasource.items.<name>.url` | 数据库连接 URL | — |
| `dynamic.datasource.items.<name>.username` | 用户名 | — |
| `dynamic.datasource.items.<name>.password` | 密码 | — |
| `dynamic.datasource.items.<name>.driverClassName` | JDBC 驱动类 | — |
| `dynamic.datasource.items.<name>.type` | 数据源实现类型（全类名，经反射实例化并用 Binder 绑定属性） | `com.zaxxer.hikari.HikariDataSource` |

> ⚠️ 密码以明文写在配置中，生产环境请配合 `security` 模块的 `ENC()` 配置解密能力使用。

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `DynamicDatasourceConfiguration` | 按配置构建多个 `DataSource`，注册单个 `DynamicDataSource` Bean（Bean 名 `dataSource`，主库由 `primary` 指定，作为 `defaultTargetDataSource`）；`@ConditionalOnProperty(prefix = "dynamic.datasource", name = "enabled", havingValue = "true")`，`@AutoConfigureBefore(DataSourceAutoConfiguration)`；同时注册 `@DynamicDatasource` 注解的 AOP 切面（order = -10） |
| `DynamicEnvPostProcessor` | 启动期扫描 `dynamic.datasource.items.*` 并补齐 `name` / `jdbcUrl` / `poolName` 属性写入 Environment（经 `spring.factories` 注册；`EnvironmentPostProcessor` 须先于自动装配阶段执行，这是 Spring Boot 3 官方约定的唯一注册方式，不可迁移到 `AutoConfiguration.imports`） |
| `DynamicDataSourceAnnotationAdvisor` / `Interceptor` | `@DynamicDatasource` 注解的 AOP 切换支持（由装配类注册，非独立自动装配） |
| `DynamicDataSource` | `AbstractRoutingDataSource` 子类，按 `DynamicDataSourceContextHolder` 当前值路由 |
| `DynamicDataSourceBuilder` | 数据源构建工具：按 `type` 反射实例化并用 `Binder` 绑定属性 |

## 继承类（业务方可实现）

| 类/接口 | 说明 |
|---------|------|
| 无强制继承 | 直接使用注解或编程式 API 即可 |

## 注解（业务方使用）

| 注解 | 说明 |
|------|------|
| `@DynamicDatasource("slave")` | 类/方法级注解（`@Target({TYPE, METHOD})`、`@Inherited`），属性 `value`（默认 `""`）；标注后方法执行期间切换到指定数据源，目标数据源不存在时抛 `BusinessException("datasource.not.exist")`；查找顺序为：方法声明接口 → 实现类方法 → 实现类 → 实现类接口 → 桥接方法及其声明类（兼容 MyBatis / MyBatis-Plus Mapper 代理）→ 父类逐级向上 → Mapper 父接口 |

> ⚠️ 注解方式仅在 AOP 代理方法调用时生效，同类内部方法直接调用不触发切换。每次切面执行（`invoke`）结束后在 `finally` 中调用 `DynamicDataSourceContextHolder.clear()` 清理 `ThreadLocal`。

> ℹ️ 历史缺陷已修复：回退逻辑曾以 `datasourceAttr != null` 判断命中（`Optional` 恒非 null），导致「父类逐级向上」「MyBatis 父接口回退」实际不生效；现改为 `Optional.isPresent()`，深层回退正常工作并有单测覆盖（`DynamicDataSourceAnnotationInterceptorTest`）。

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `DynamicDataSourceContextHolder.setDataSource(name)` | 编程式切换数据源（`ThreadLocal`）；`getDataSource()` 查询当前；`clear()` 清理；`addDataSource(name)` 动态添加数据源名；`containsDataSource(name)` 判断是否已添加 |
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
// 1) 注解方式切换（方法级 / 类级均可）
@DynamicDatasource("slave")
public List<Order> listFromSlave() { ... }

// 2) 编程式切换（用完记得 clear）
DynamicDataSourceContextHolder.setDataSource("slave");
try { ... } finally { DynamicDataSourceContextHolder.clear(); }
```

> 注：`spring.factories` 中 `DynamicEnvPostProcessor` 会为每个数据源补齐 `name` / `jdbcUrl` / `poolName` 属性写入 Environment。其中 `jdbcUrl`（取自配置的 `url`）才是 `HikariDataSource` 等连接池实际可绑定的 URL 属性——`HikariConfig` 仅提供 `setJdbcUrl`，不存在 `setUrl`，配置中的 `url` 本身不会被直接绑定；`poolName` 用于连接池命名。`username` / `password` / `driverClassName` 等原始属性由 `Binder` 在绑定 `dynamic.datasource.items.<name>` 前缀时按宽松绑定规则映射，无对应 setter 的属性（如 `name`）会被忽略。
