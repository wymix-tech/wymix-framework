package org.wymix.framework.security.handler;

import org.wymix.framework.common.result.ResultVO;
import org.wymix.framework.utils.HttpUtils;
import org.wymix.framework.utils.json.JsonUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BaseAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        HttpUtils.response(response, 403, JsonUtils.entityToJson(ResultVO.of(403, "access.denied")));
    }
}
