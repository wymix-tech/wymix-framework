package com.swcares.sample.demo.security;

import org.wymix.framework.security.helper.JwtTokenHelper;
import org.wymix.framework.security.properties.CustomSecurityProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * security 示例：JWT 签发与校验。
 *
 * <p>密钥与有效期取自 {@code security.jwt.*}（见 {@code application-security.yml}），
 * 框架登录成功后由 {@code LoginSuccessHandler} 使用同一工具类签发令牌，
 * {@code JwtAuthenticationFilter} 负责按请求头校验。
 */
@RestController
@RequestMapping("demo/security")
@Tag(name = "security 模块示例")
public class JwtDemoController {

    private final CustomSecurityProperties securityProperties;

    public JwtDemoController(CustomSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @PostMapping("token")
    @Operation(summary = "模拟登录并签发 JWT")
    public Map<String, Object> token(@RequestParam(defaultValue = "admin") String username) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username, "N/A", List.of(new SimpleGrantedAuthority("superadmin")));
        JwtTokenHelper helper = new JwtTokenHelper(
                securityProperties.getJwt().getSecret(), securityProperties.getJwt().getExpiredTime());
        String token = helper.generateToken(authentication);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("username", helper.extractUsername(token));
        result.put("valid", helper.validateToken(token));
        result.put("expiration", helper.extractExpiration(token));
        return result;
    }

    @GetMapping("verify")
    @Operation(summary = "校验 JWT 是否有效")
    public boolean verify(@RequestParam String token) {
        JwtTokenHelper helper = new JwtTokenHelper(
                securityProperties.getJwt().getSecret(), securityProperties.getJwt().getExpiredTime());
        return helper.validateToken(token);
    }
}
