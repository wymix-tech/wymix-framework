package com.travelsky.base.security.handler;

import com.travelsky.base.common.result.ResultVO;
import com.travelsky.base.utils.HttpUtils;
import com.travelsky.base.utils.json.JsonUtils;
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
