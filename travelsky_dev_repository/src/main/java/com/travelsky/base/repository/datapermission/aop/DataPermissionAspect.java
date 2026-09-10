package com.travelsky.base.repository.datapermission.aop;

import com.travelsky.base.repository.datapermission.annotations.DataPermission;
import com.travelsky.base.repository.datapermission.context.DataPermissionContextHolder;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.ArrayList;

/**
 * 数据权限切面：将 {@code @DataPermission} 声明的注入器写入线程上下文。
 * MyBatis 侧由 {@code DataPermissionPlugin} 消费；JPA 侧由 {@code SpecificationHelper#applyDataPermission} 消费。
 */
@Aspect
public class DataPermissionAspect {

    @Around("@annotation(com.travelsky.base.repository.datapermission.annotations.DataPermission) && @annotation(dataPermission)")
    public Object around(ProceedingJoinPoint point, DataPermission dataPermission) throws Throwable {

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
            DataPermissionContextHolder.clear();
        }
    }
}
