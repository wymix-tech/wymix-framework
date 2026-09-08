package com.travelsky.base.web.aop;

import com.travelsky.base.common.consts.SystemConstant;
import com.travelsky.base.common.exception.BusinessException;
import com.travelsky.base.web.context.RequestContextHolder;
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

    private static final ConcurrentMap<String, Long> MAP = new ConcurrentHashMap<>();

    /**
     * 使用纯类型匹配的 {@code @annotation(全限定名)} 切点，注解实例通过反射获取。
     *
     * <p>不采用 {@code @annotation(repeatRequest)} 参数绑定：在 CGLIB 代理下 Spring 需要额外绑定
     * {@code JoinPointMatch}，否则运行期抛
     * {@code IllegalStateException: Required to bind 2 arguments, but only bound 1}。
     */
    @Around("@annotation(com.travelsky.base.web.aop.RepeatRequest)")
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
        // 必须用 sb.toString().hashCode()：StringBuilder 未重写 hashCode()，
        // 直接取 sb.hashCode() 得到的是身份哈希，每次请求都不同，去重键永远无法命中
        String name = className + methodName + sb.toString().hashCode();
        Long lastTime = (Long) session.getAttribute(name);
        long now = System.currentTimeMillis();
        if (lastTime == null) {
            session.setAttribute(name, now);
            return point.proceed();
        }
        // 距上次提交的间隔小于去重窗口 -> 判定为重复提交
        long elapsed = now - lastTime;
        if (elapsed < repeatRequest.expiredTimeInMills()) {
            // 重复提交属于客户端问题，用 400（数据校验类错误码）而非默认的 500
            throw new BusinessException(SystemConstant.ERROR_CODE_DATA_VALIDATION,
                    "repeat.request", repeatRequest.expiredTimeInMills());
        }
        // 超出窗口，放行并刷新时间戳
        session.setAttribute(name, now);
        return point.proceed();
    }
}
