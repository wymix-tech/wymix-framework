package com.swcares.sample.secure;

import org.wymix.framework.common.context.Principal;
import org.wymix.framework.common.context.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * security 示例：<b>受保护</b>端点。
 *
 * <p>与 {@code /demo/**}（已在 {@code security.ignoreUrls} 放行）不同，{@code /secure/**}
 * 不在白名单内，必须携带有效凭证才能访问：
 * <ol>
 *     <li>表单登录：{@code POST /login?username=admin&password=Abc@123}
 *     （用户见 {@code SecurityConfiguration}，密码编码器由 security 模块提供）；</li>
 *     <li>JWT：登录成功后 {@code LoginSuccessHandler} 签发令牌，后续请求带上该令牌即可，
 *     签发逻辑见 {@code /demo/security/token}。</li>
 * </ol>
 * 未认证访问会由 {@code Http401UnauthorizedEntryPoint} 返回 401。
 */
@RestController
@RequestMapping("secure")
@Tag(name = "security 受保护端点示例")
public class SecureDemoController {

    @GetMapping("me")
    @Operation(summary = "查看当前登录身份（需先登录）")
    public Map<String, Object> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> result = new LinkedHashMap<>();
        if (authentication == null) {
            result.put("authenticated", false);
            return result;
        }
        result.put("authenticated", true);
        result.put("name", authentication.getName());
        result.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return result;
    }

    @GetMapping("context")
    @Operation(summary = "UserContextHolder：审计字段自动填充使用同一份上下文")
    public Map<String, Object> context() {
        Principal<String> principal = UserContextHolder.getUserContext();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("principal", principal == null ? null : principal.getPrincipal());
        result.put("source", principal == null ? null : principal.getSource());
        return result;
    }
}
