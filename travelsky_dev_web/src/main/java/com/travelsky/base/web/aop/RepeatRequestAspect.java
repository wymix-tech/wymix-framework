package com.travelsky.base.web.aop;

import com.travelsky.base.common.exception.BusinessException;
import com.travelsky.base.web.context.RequestContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RepeatRequestAspect {

    private static final Logger log = LoggerFactory.getLogger(RepeatRequestAspect.class);

    private static final ConcurrentMap<String, Long> MAP = new ConcurrentHashMap<>();

    @Around("@annotation(com.travelsky.plc.ams.core.web.aop.RepeatRequest) && @annotation(repeatRequest)")

    public Object invoke(ProceedingJoinPoint point, RepeatRequest repeatRequest) throws Throwable {

        if (!point.getTarget().getClass().isAnnotationPresent(Controller.class)) {
            log.warn("RepeatRequestAspect just support controller!");
            return point.proceed();
        }
        HttpServletRequest request = RequestContextHolder.getRequest();
        if (request == null) {
            log.warn("RepeatRequestAspect just support web env!");
            return point.proceed();
        }

        HttpSession session = request.getSession();

        String className = point.getSignature().getDeclaringTypeName();
        String methodName = point.getSignature().getName();
        MethodSignature signature = (MethodSignature) point.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = point.getArgs();
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (String parameterName : parameterNames) {
            sb.append(parameterName).append(":").append(Optional.ofNullable(args[index++]).orElse("null").toString()).append(";");
        }
        String name = className + methodName + sb.hashCode();
        Long attribute = (Long) session.getAttribute(name);
        if (attribute == null) {
            session.setAttribute(name, System.currentTimeMillis());
            return point.proceed();
        }
        long delta = attribute - System.currentTimeMillis();
        if (delta > repeatRequest.expiredTimeInMills()) {
            session.setAttribute(name, System.currentTimeMillis());
            return point.proceed();
        }
        throw new BusinessException("repeat.request", repeatRequest.expiredTimeInMills());
    }
}
