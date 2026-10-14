package org.wymix.framework.security.entrypoint;

import org.wymix.framework.common.result.ResultVO;
import org.wymix.framework.utils.HttpUtils;
import org.wymix.framework.utils.json.JsonUtils;
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
