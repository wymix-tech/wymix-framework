package com.travelsky.base.security.handler;

import com.travelsky.base.common.result.ResultVO;
import com.travelsky.base.security.helper.JwtTokenHelper;
import com.travelsky.base.utils.HttpUtils;
import com.travelsky.base.utils.json.JsonUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录成功：签发 JWT 并返回。
 *
 * <p>依赖通过构造器注入：本类由 {@code CustomSecurityConfiguration} 用 {@code new} 创建，
 * 若使用 {@code @Autowired} 字段注入，Spring 不会处理，字段会一直为 null。
 */
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenHelper jwtTokenHelper;

    public LoginSuccessHandler(JwtTokenHelper jwtTokenHelper) {
        this.jwtTokenHelper = jwtTokenHelper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        HttpUtils.response(response, 200, JsonUtils.entityToJson(ResultVO.of(jwtTokenHelper.generateToken(authentication))));
    }
}
