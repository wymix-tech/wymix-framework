package org.wymix.framework.security.handler;

import org.wymix.framework.security.context.JwtTokenBlackList;
import org.wymix.framework.security.helper.JwtTokenHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 登出时把当前令牌加入黑名单，避免登出后令牌仍能使用。
 *
 * <p>依赖通过构造器注入，原因同 {@link LoginSuccessHandler}。
 */
public class InvalidJwtTokenLogoutHandler implements LogoutHandler {

    private final JwtTokenHelper jwtTokenHelper;

    private final JwtTokenBlackList jwtTokenBlackList;

    public InvalidJwtTokenLogoutHandler(JwtTokenHelper jwtTokenHelper, JwtTokenBlackList jwtTokenBlackList) {
        this.jwtTokenHelper = jwtTokenHelper;
        this.jwtTokenBlackList = jwtTokenBlackList;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null) {
            return;
        }
        // 与 JwtAuthenticationFilter 保持一致：去掉 "Bearer " 前缀后再入黑名单，
        // 否则 key 不匹配，登出后令牌依然可用
        jwtTokenBlackList.add(authorization.replaceFirst("^Bearer\\s+", ""), jwtTokenHelper.getExpirationTime());
    }
}
