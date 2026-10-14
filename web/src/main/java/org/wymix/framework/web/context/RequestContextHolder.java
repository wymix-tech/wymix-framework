package org.wymix.framework.web.context;

import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

public class RequestContextHolder {

    private RequestContextHolder() {

    }

    public static final HttpServletRequest getRequest() {
        Optional<ServletRequestAttributes> requestAttributes = Optional.ofNullable((ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes());
        return requestAttributes.isPresent() ? requestAttributes.get().getRequest() : null;
    }

    public static final HttpServletResponse getResponse() {
        Optional<ServletRequestAttributes> requestAttributes = Optional.ofNullable((ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes());
        return requestAttributes.isPresent() ? requestAttributes.get().getResponse() : null;
    }
}
