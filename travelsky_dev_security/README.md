# travelsky_dev_security 安全模块

> 认证与访问控制：JWT 无状态认证、令牌黑名单、配置项 `ENC()` 解密与 BCrypt 加密。

## 模块说明

`travelsky_dev_security` 提供基于 Spring Security 的企业认证能力。核心是 `CustomSecurityConfiguration`（抽象类，业务方继承后注入 `UserDetailsService` 即可）；支持表单登录与 JWT 两种模式；配置中的密码若以 `ENC(...)` 包裹则自动解密；登录凭证使用 BCrypt 加密；JWT 提供令牌黑名单机制实现主动失效。

## 功能说明

| 能力 | 关键类 | 说明 |
|------|--------|------|
| 认证配置 | `CustomSecurityConfiguration`（抽象） | 业务继承后注入 `UserDetailsService` 即可启用安全 |
| JWT | `CustomSecurityProperties.JwtProperties` | 无状态令牌认证，支持 `randomSecret` / 固定 `secret` / 过期时间 |
| 令牌黑名单 | JWT 黑名单组件 | 实现令牌主动失效 |
| 配置解密 | `ENC(...)` 解析 | 配置文件中密码用 `ENC(...)` 包裹时自动解密 |
| 密码加密 | BCrypt | 默认 `passwordEncoder` 为 BCrypt |
| 白名单 | `ignoreUrls` | 配置无需认证的路径 |
| 扩展点 | `SecurityCustomizer` | 提供安全过滤链定制入口 |

## 配置项

配置前缀：`security`

| 配置项 | 说明 | 默认 |
|--------|------|------|
| `security.formLoginEnable` | 是否启用表单登录 | true |
| `security.loginPage` | 自定义登录页路径 | — |
| `security.loginProcessingUrl` | 登录处理 URL | `/login` |
| `security.logoutUrl` | 登出 URL | `/logout` |
| `security.ignoreUrls` | 放行 URL（逗号分隔） | — |
| `security.jwt.randomSecret` | 启动时随机生成密钥 | false |
| `security.jwt.secret` | JWT 固定密钥（生产建议配置） | — |
| `security.jwt.expiredTime` | 令牌过期时间（毫秒） | 0 |

> 自动配置通过旧式 `META-INF/spring.factories` 注册（与框架其余模块的新式 `AutoConfiguration.imports` 不一致，属已知差异）。

## 使用示例

```java
// 1) 继承 CustomSecurityConfiguration 并注入 UserDetailsService
@Configuration
public class SecurityConfiguration extends CustomSecurityConfiguration {
    @Bean
    public InMemoryUserDetailsManager userDetailsManager() {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin")
                .password("Abc@123")
                .passwordEncoder(s -> passwordEncoder().encode(s))
                .authorities("superadmin")
                .build());
    }
}
```

```yaml
# application-security.yml
security:
  formLoginEnable: true
  ignoreUrls: /actuator/**,/swagger-ui/**,/doc.html
  jwt:
    randomSecret: false
    secret: ${JWT_SECRET}
    expiredTime: 7200000
spring:
  datasource:
    password: ENC(密文)     # 自动解密
```

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_security</artifactId>
    <version>${revision}</version>
</dependency>
```
