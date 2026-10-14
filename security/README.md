# security 安全模块

> **当前版本兼容变化**：请先阅读 [评审修复与接入说明](../docs/REVIEW_REPAIR.md)。新项目推荐 MyBatis，JPA 仅兼容维护。

> 认证与访问控制：JWT 无状态认证、令牌黑名单、配置项 `ENC()` 解密与 BCrypt 加密。

## 模块说明

`security` 提供基于 Spring Security 的企业认证能力。是否装配由 `security.enabled` 控制（默认开启）；表单登录模式需提供 `UserDetailsService` Bean 作为用户来源，无需继承任何配置类；支持表单登录与 JWT 两种模式；配置中的值若以 `ENC(...)` 包裹则自动 Base64 解码（不提供保密性）（`DecryptEnvironmentPostProcessor`）；登录凭证使用 BCrypt 密码哈希；JWT 提供令牌黑名单机制实现登出后主动失效（黑名单为单机内存态 `JwtTokenBlackList`，不含分布式共享存储，登出记入的失效时长为该签名令牌 exp 的剩余寿命）。

## 依赖引入

```xml
<dependency>
    <groupId>org.wymix.framework</groupId>
    <artifactId>security</artifactId>
    <version>${revision}</version>
</dependency>
```

> JWT 依赖（`jjwt-api` / `jjwt-impl` / `jjwt-jackson`，scope：impl 与 jackson 为 `runtime`）版本由父 POM 统一管理，当前为 `0.13.0`。

## 配置项

配置前缀：`security`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `security.enabled` | 是否启用安全自动装配 | `true` |
| `security.formLoginEnable` | 是否启用表单登录 | `true` |
| `security.loginPage` | 自定义登录页路径 | — |
| `security.loginProcessingUrl` | 登录处理 URL | `/login` |
| `security.logoutUrl` | 登出 URL | `/logout` |
| `security.ignoreUrls` | 放行 URL（逗号分隔） | — |
| `security.jwt.randomSecret` | 启动时随机生成密钥 | `false` |
| `security.jwt.secret` | JWT 固定密钥（生产建议配置） | — |
| `security.jwt.expiredTime` | 令牌过期时间（秒） | 3600 |

## 集成类（自动装配）

| 类 | 说明 |
|----|------|
| `CustomSecurityConfiguration` | 自动装配入口：`JwtTokenHelper`、`JwtAuthenticationFilter`、`JwtTokenBlackList` 等 |
| `DecryptEnvironmentPostProcessor` | `ENC(...)` 配置解密（经 `spring.factories` 注册，先于自动装配执行） |
| `JwtAuthenticationFilter` | JWT 认证过滤器 |
| `LoginSuccessHandler` / `LoginFailureHandler` | 登录成功/失败处理器 |
| `Http401UnauthorizedEntryPoint` / `BaseAccessDeniedHandler` | 401 入口点与 403 处理器 |
| `InvalidJwtTokenLogoutHandler` | 登出时把 JWT 加入黑名单（内存态，失效时长取签名 exp 的剩余寿命） |

## 继承类（业务方核心用法）

| 类/接口 | 说明 |
|---------|------|
| `CustomSecurityConfiguration` | 自动装配入口（`@AutoConfiguration`，配合 `SecurityFilterChain` 构建），非继承用途；业务方只需声明 `UserDetailsService` Bean |
| `SecurityCustomizer` | **函数式扩展接口**：`customize(HttpSecurity)` 自定义安全过滤链，声明 Bean 即被收集调用 |
| `JwtTokenBlackList` | 令牌黑名单可自行实现/覆盖（`@ConditionalOnMissingBean`） |

## 扩展点

| 扩展点 | 说明 |
|--------|------|
| `UserDetailsService` Bean | 提供用户来源（内存/数据库），启用认证 |
| `SecurityCustomizer` Bean | 定制 `HttpSecurity`（权限规则、OAuth2 等） |
| 标准处理器 Bean 覆盖 | `AuthenticationEntryPoint` / `AuthenticationSuccessHandler` / `AuthenticationFailureHandler` / `AccessDeniedHandler` / `LogoutSuccessHandler` 等均 `@ConditionalOnMissingBean`，声明即覆盖 |
| `JwtTokenHelper` | 令牌签发与校验工具，可注入使用 |

## 使用示例

```java
// 新版用法：提供 UserDetailsService Bean 即启用表单登录，无需继承配置类
@Configuration
public class SecurityConfiguration {

    @Bean
    public InMemoryUserDetailsManager userDetailsManager(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin")
                .password("Abc@123")
                .passwordEncoder(s -> encoder.encode(s))
                .authorities("superadmin")
                .build());
    }
}
```

```yaml
# application-security.yml
security:
  formLoginEnable: true
  ignoreUrls: /swagger-ui/**,/doc.html
  jwt:
    randomSecret: false
    secret: ${JWT_SECRET}
    expiredTime: 7200
spring:
  datasource:
    password: ${DB_PASSWORD}  # 生产通过 Secret 管理
```

> 自动配置同时注册于 `META-INF/spring.factories`（仅 `EnvironmentPostProcessor`）与 `AutoConfiguration.imports`。
