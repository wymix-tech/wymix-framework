package org.wymix.framework.security.handler;

import org.wymix.framework.common.result.ResultVO;
import org.wymix.framework.utils.HttpUtils;
import org.wymix.framework.utils.json.JsonUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        HttpUtils.response(response, 500, JsonUtils.entityToJson(ResultVO.of(500, "login.fail")));
    }
}
