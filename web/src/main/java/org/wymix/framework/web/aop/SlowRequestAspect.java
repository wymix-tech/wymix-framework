package org.wymix.framework.web.aop;

import org.wymix.framework.web.context.RequestContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SlowRequestAspect {

    private static final Logger log = LoggerFactory.getLogger(SlowRequestAspect.class);

    @Value("${app.slow.request.inSecond:5}")
    private int slowSecond;


    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || @within(org.springframework.web.bind.annotation.RequestMapping)")
    public Object invoke(ProceedingJoinPoint point) throws Throwable {
        long cur = System.currentTimeMillis();
        Object proceed = point.proceed();
        long delta = System.currentTimeMillis() - cur;
        if (delta > slowSecond * 1000) {
            HttpServletRequest request = RequestContextHolder.getRequest();
            MethodSignature signature = (MethodSignature) point.getSignature();
            String method = new StringBuilder(point.getTarget().getClass().getSimpleName()).append(".").append(signature.getMethod().getName()).toString();
            String url = "";
            if (request != null) {
                url = request.getRequestURL().toString();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("###请求URL: " + url);
            sb.append("   ###IP: " + request.getRemoteAddr());
            sb.append("   ###Params: " + Arrays.toString(point.getArgs()));
            sb.append("   ###CLASS_METHOD: " + method);
            sb.append("   ###耗时: " + delta + "毫秒");
            log.warn(sb.toString());
        }
        return proceed;
    }
}
