package com.travelsky.base.repository.datapermission.aop;

import com.travelsky.base.repository.datapermission.annotations.DataPermission;
import com.travelsky.base.repository.datapermission.context.DataPermissionContextHolder;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.ArrayList;

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
