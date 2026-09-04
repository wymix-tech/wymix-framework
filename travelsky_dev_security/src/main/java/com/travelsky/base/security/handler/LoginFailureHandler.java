package com.travelsky.base.security.handler;

import com.travelsky.base.common.result.ResultVO;
import com.travelsky.base.utils.HttpUtils;
import com.travelsky.base.utils.json.JsonUtils;
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
