package com.travelsky.base.security.entrypoint;

import com.travelsky.base.common.result.ResultVO;
import com.travelsky.base.utils.HttpUtils;
import com.travelsky.base.utils.json.JsonUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Http401UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        HttpUtils.response(response, 401, JsonUtils.entityToJson(ResultVO.of(401, "system.unAuthorize")));
    }
}
