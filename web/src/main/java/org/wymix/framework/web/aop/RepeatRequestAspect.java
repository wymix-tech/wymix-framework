package org.wymix.framework.web.aop;

import org.wymix.framework.common.consts.SystemConstant;
import org.wymix.framework.common.exception.BusinessException;
import org.wymix.framework.web.context.RequestContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
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

    private static final com.fasterxml.jackson.databind.ObjectMapper KEY_MAPPER = com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .enable(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
    private static final String SESSION_KEYS = RepeatRequestAspect.class.getName() + ".keys";

    /**
     * 使用纯类型匹配的 {@code @annotation(全限定名)} 切点，注解实例通过反射获取。
     *
     * <p>不采用 {@code @annotation(repeatRequest)} 参数绑定：在 CGLIB 代理下 Spring 需要额外绑定
     * {@code JoinPointMatch}，否则运行期抛
     * {@code IllegalStateException: Required to bind 2 arguments, but only bound 1}。
     */
    @Around("@annotation(org.wymix.framework.web.aop.RepeatRequest)")
    public Object invoke(ProceedingJoinPoint point) throws Throwable {

        RepeatRequest repeatRequest = AnnotatedElementUtils.findMergedAnnotation(
                ((MethodSignature) point.getSignature()).getMethod(), RepeatRequest.class);
        if (repeatRequest == null) {
            return point.proceed();
        }

        // 用 AnnotatedElementUtils 识别元注解，@RestController 是 @Controller 的派生注解
        if (!AnnotatedElementUtils.hasAnnotation(point.getTarget().getClass(), Controller.class)) {
            log.warn("RepeatRequestAspect just support controller!");
            return point.proceed();
        }
        HttpServletRequest request = RequestContextHolder.getRequest();
        if (request == null) {
            log.warn("RepeatRequestAspect just support web env!");
            return point.proceed();
        }

        HttpSession session = request.getSession();

        MethodSignature signature = (MethodSignature) point.getSignature();
        Object[] meaningfulArgs = java.util.Arrays.stream(point.getArgs())
                .filter(arg -> !(arg instanceof jakarta.servlet.ServletRequest)
                        && !(arg instanceof jakarta.servlet.ServletResponse)
                        && !(arg instanceof org.springframework.validation.BindingResult)).toArray();
        byte[] json = KEY_MAPPER.writeValueAsBytes(meaningfulArgs);
        String digest = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json));
        String name = signature.getMethod().toGenericString() + ":" + digest;
        long now = System.currentTimeMillis();
        synchronized (org.springframework.web.util.WebUtils.getSessionMutex(session)) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Long> keys = (java.util.Map<String, Long>) session.getAttribute(SESSION_KEYS);
            if (keys == null) { keys = new java.util.LinkedHashMap<>(); session.setAttribute(SESSION_KEYS, keys); }
            keys.entrySet().removeIf(entry -> entry.getValue() <= now);
            if (keys.containsKey(name)) {
                throw new BusinessException(SystemConstant.ERROR_CODE_DATA_VALIDATION, "repeat.request", repeatRequest.expiredTimeInMills());
            }
            if (keys.size() >= 1000) keys.remove(keys.keySet().iterator().next());
            keys.put(name, now + repeatRequest.expiredTimeInMills());
        }
        return point.proceed();
    }
}
