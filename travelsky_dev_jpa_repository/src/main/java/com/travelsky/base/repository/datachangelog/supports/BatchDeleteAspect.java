/*
 * Copyright (c) 2025, TravelSky.
 *  All rights reserved.
 *  TravelSky Confidential.
 */
package com.travelsky.base.repository.datachangelog.supports;

import com.travelsky.base.repository.datachangelog.dto.DataChangeLogDTO;
import com.travelsky.base.repository.datachangelog.enums.StatementType;
import com.travelsky.base.repository.datachangelog.event.DataChangeLogEvent;
import com.travelsky.base.utils.ReflectUtils;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The type Batch delete aspect.
 *
 * @author xnky_wangyang01
 * @version 1.0
 * @date 2025 -08-11
 * @copyright 中国航信
 */
@Aspect
public class BatchDeleteAspect {

    /**
     * The constant PATTERN.
     */
    private static final Pattern PATTERN = Pattern.compile(":(\\S+?)(?=[,\\s]|$)");

    /**
     * The Entity manager.
     */
    @Autowired
    private volatile EntityManager entityManager;

    @Autowired
    private AuditorAware<String> auditorAware;

    /**
     * The Mutex.
     */
    private Object mutex = new Object();

    /**
     * The Application event publisher.
     */
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;


    /**
     * Proceed object.
     *
     * @param point the point
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.*InBatch(..))")
    public Object proceed(ProceedingJoinPoint point) throws Throwable {
        try {
            if (!DataChangeLogListener.isWebRequest()) {
                return point.proceed();
            }
            return record(point);
        } finally {
            ManualDmlContext.clear();
        }
    }

    /**
     * Record object.
     *
     * @param point the point
     * @return the object
     * @throws Throwable the throwable
     */
    private Object record(ProceedingJoinPoint point) throws Throwable {
        Object[] args = point.getArgs();
        JpaEntityInformation entityInformation = (JpaEntityInformation) ReflectUtils.getFieldValue(AopProxyUtils.getSingletonTarget(point.getTarget()), "entityInformation");
        Map<String, Object> params = ManualDmlContext.createParams();
        if (args != null && args.length > 0) {
            Object id;
            Iterable iterable = (Iterable) args[0];
            int i = 1;
            for (Object o : iterable) {
                if (o.getClass().equals(entityInformation.getJavaType())) {
                    id = entityInformation.getId(o);
                } else {
                    id = o;
                }
                params.put(i++ + "", id);
            }
        }

        Object object = point.proceed();

        Map<String, Object> results = ManualDmlContext.getResults();
        PlainSelect select = (PlainSelect) results.get("sql");
        Field[] fields = ReflectUtils.getFieldsDirectly(entityInformation.getJavaType(), true);
        fields = Arrays.stream(fields).filter(field -> Modifier.isPrivate(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())).toArray(Field[]::new);
        LocalDateTime now = LocalDateTime.now();
        if (results != null && !results.isEmpty()) {
            List<Object> oldResults = (List<Object>) results.get("results");
            DataChangeLogDTO dto;
            for (Object oldResult : oldResults) {
                dto = new DataChangeLogDTO();
                dto.setUri(DataChangeLogListener.getUri());
                dto.setRequestId(DataChangeLogListener.getRequestId());
                dto.setRequestRemoteIp(DataChangeLogListener.getRequestRemoteIp());
                dto.setClassName(entityInformation.getJavaType().getName());
                dto.setTableName(select.getFromItem().toString());
                dto.setId(entityInformation.getId(oldResult));
                dto.setStatementType(StatementType.REMOVE);
                dto.setLastModifiedBy(auditorAware.getCurrentAuditor().orElse("SYSTEM"));
                dto.setLastModifiedDate(now);
                dto.setFieldInfos(Arrays.stream(fields).map(field -> {
                    DataChangeLogDTO.FieldInfo fieldInfo = new DataChangeLogDTO.FieldInfo();
                    fieldInfo.setField(field.getName());
                    fieldInfo.setOldValue(ReflectUtils.getFieldValue(oldResult, field));
                    return fieldInfo;
                }).toList());
                applicationEventPublisher.publishEvent(new DataChangeLogEvent(this, dto));
            }
        }
        return object;
    }
}
