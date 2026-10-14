package org.wymix.framework.repository.datachangelog.supports;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/** Legacy compatibility type. Not auto-registered; manual DML auditing is unsupported. */
@Deprecated(since = "3.0", forRemoval = false)
@Aspect
public class BatchDeleteAspect {
    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.*InBatch(..))")
    public Object proceed(ProceedingJoinPoint point) throws Throwable {
        throw new UnsupportedOperationException("JPA manual/batch DML auditing is unsupported; use mybatis_repository");
    }
}
