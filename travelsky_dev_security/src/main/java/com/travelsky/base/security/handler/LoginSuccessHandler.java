package com.travelsky.base.security.handler;

import com.travelsky.base.common.result.ResultVO;
import com.travelsky.base.security.helper.JwtTokenHelper;
import com.travelsky.base.utils.HttpUtils;
import com.travelsky.base.utils.json.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private JwtTokenHelper jwtTokenHelper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        HttpUtils.response(response, 200, JsonUtils.entityToJson(ResultVO.of(jwtTokenHelper.generateToken(authentication))));
    }
}
