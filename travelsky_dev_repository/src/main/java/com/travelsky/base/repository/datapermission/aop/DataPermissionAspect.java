package com.travelsky.base.repository.datapermission.aop;

import com.travelsky.base.repository.datapermission.annotations.DataPermission;
import com.travelsky.base.repository.datapermission.context.DataPermissionContextHolder;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;

/**
 * 数据权限切面：将 {@code @DataPermission} 声明的注入器写入线程上下文。
 * MyBatis 侧由 {@code DataPermissionPlugin} 消费；JPA 侧由 {@code SpecificationHelper#applyDataPermission} 消费。
 *
 * <p><b>退出时恢复而非直接 clear</b>：进入方法前若已有上下文（嵌套 {@code @DataPermission}、
 * 或同一方法被两个切面实例环绕——MyBatis 侧 {@code @EnableDataPermission} 与本切面同时存在时可能出现），
 * 直接 {@code clear()} 会让外层/内层切面丢失条件，表现为「数据权限静默失效、返回全量数据」。
 * 因此这里在 {@code finally} 中恢复进入前的值（无则 remove），消除顺序与重复注册带来的不确定性。
 *
 * <p><b>切点用纯类型匹配</b>：不采用 {@code @annotation(dataPermission)} 形式参数绑定。
 * 参数绑定要求调用期额外绑定 {@code JoinPointMatch}，在 JDK 动态代理（目标类实现了接口）下会抛
 * {@code IllegalStateException: Required to bind 2 arguments, but only bound 1}；
 * 这里改为在方法体内按 {@link MethodSignature} 取注解（与 {@code RepeatRequestAspect} 一致），
 * 两种代理方式均可正常工作。
 *
 * <p>本类只标注 {@code @Aspect}，未声明 {@code @EnableAspectJAutoProxy}：目前由 core 的
 * {@code AsyncScheduleAutoConfiguration}（{@code @EnableAsync(proxyTargetClass = true)}）间接开启 AOP 代理；
 * 若业务未引入 core，请在自己的配置类上显式加 {@code @EnableAspectJAutoProxy}。
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataPermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(DataPermissionAspect.class);

    @Around("@annotation(com.travelsky.base.repository.datapermission.annotations.DataPermission)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        DataPermission dataPermission = resolveAnnotation(point);
        if (dataPermission == null) {
            return point.proceed();
        }
        DataPermissionContextHolder.DataPermissionContext previous = DataPermissionContextHolder.getContext();
        try {
            DataPermissionContextHolder.DataPermissionContext context = new DataPermissionContextHolder.DataPermissionContext();
            context.setStatementIds(dataPermission.statementIds());
            context.setInjectors(new ArrayList<>());
            for (Class<? extends PermissionSqlInjector> injector : dataPermission.injectors()) {
                context.getInjectors().add(injector);
            }
            DataPermissionContextHolder.mark(context);
            return point.proceed();
        } finally {
            if (previous == null) {
                DataPermissionContextHolder.clear();
            } else {
                DataPermissionContextHolder.mark(previous);
                log.debug("数据权限上下文已恢复外层 @DataPermission 声明，避免嵌套/重复切面导致条件丢失");
            }
        }
    }

    /** 从被代理方法上读取 {@code @DataPermission}（支持接口方法上的注解）。 */
    private static DataPermission resolveAnnotation(ProceedingJoinPoint point) {
        if (!(point.getSignature() instanceof MethodSignature signature)) {
            return null;
        }
        return AnnotatedElementUtils.findMergedAnnotation(signature.getMethod(), DataPermission.class);
    }
}
