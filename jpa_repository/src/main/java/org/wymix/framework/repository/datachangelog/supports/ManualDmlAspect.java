package org.wymix.framework.repository.datachangelog.supports;

import org.wymix.framework.common.exception.BusinessException;
import org.wymix.framework.repository.datachangelog.dto.DataChangeLogDTO;
import org.wymix.framework.repository.datachangelog.enums.StatementType;
import org.wymix.framework.repository.datachangelog.event.DataChangeLogEvent;
import org.wymix.framework.utils.ReflectUtils;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Aspect
public class ManualDmlAspect {

    private static final Pattern PATTERN = Pattern.compile(":(\\S+?)(?=[,\\s]|$)");

    @Autowired
    private volatile EntityManager entityManager;

    @Autowired
    private AuditorAware<String> auditorAware;

    private Object mutex = new Object();

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Around("@annotation(org.springframework.data.jpa.repository.Modifying) && @annotation(query)")
    public Object proceed(ProceedingJoinPoint point, Query query) throws Throwable {

        try {
            return record(point, query);
        } finally {
            ManualDmlContext.clear();
        }
    }

    private Object record(ProceedingJoinPoint point, Query query) throws Throwable {
        Map<String, Object> map = new HashMap<>();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = point.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];
            if (parameter.isAnnotationPresent(Param.class)) {
                Param param = parameter.getDeclaredAnnotation(Param.class);
                if (StringUtils.isNotBlank(param.value())) {
                    map.put(param.value(), arg);
                    continue;
                }
                map.put(parameter.getName(), arg);
            }
        }

        String sql = query.value();

        Matcher matcher = PATTERN.matcher(sql);
        int i = 1;
        Map<String, Object> params = ManualDmlContext.createParams();
        while (matcher.find()) {
            params.put(i++ + "", map.get(matcher.group(1)));
        }
        Object proceed = point.proceed();

        Map<String, Object> results = ManualDmlContext.getResults();

        if (results != null && !results.isEmpty()) {
            PlainSelect select = (PlainSelect) results.get("sql");
            Class entityClass = (Class) results.get("entityClass");
            jakarta.persistence.Query nativeQuery = entityManager.createNativeQuery(select.toString(), entityClass);

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String k = entry.getKey();
                if (select.getWhere().toString().contains(":" + k + " ") || select.getWhere().toString().endsWith(":" + k)) {
                    nativeQuery.setParameter(k, entry.getValue());
                }
            }
            List<Object> newResults = nativeQuery.getResultList();
            List<Object> oldResults = (List<Object>) results.get("results");

            Field[] fields = ReflectUtils.getFieldsDirectly(entityClass, true);
            Field idField = Arrays.stream(fields).filter(field -> field.isAnnotationPresent(Id.class)).findFirst().orElseThrow(() -> new BusinessException("1212", "3232"));
            fields = Arrays.stream(fields).filter(field -> Modifier.isPrivate(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())).toArray(Field[]::new);
            DataChangeLogDTO dto;
            Optional<Object> optional;
            for (Object newResult : newResults) {
                optional = oldResults.stream().filter(r -> ReflectUtils.getFieldValue(newResult, idField).equals(ReflectUtils.getFieldValue(r, idField))).findFirst();
                if (optional.isPresent()) {
                    Object oldResult = optional.get();
                    dto = new DataChangeLogDTO();
                    dto.setUri(DataChangeLogListener.getUri());
                    dto.setRequestId(DataChangeLogListener.getRequestId());
                    dto.setRequestRemoteIp(DataChangeLogListener.getRequestRemoteIp());
                    dto.setClassName(entityClass.getName());
                    dto.setTableName(select.getFromItem().toString());
                    dto.setId(ReflectUtils.getFieldValue(newResult, idField));
                    dto.setStatementType(StatementType.MODIFY);
                    dto.setLastModifiedBy(auditorAware.getCurrentAuditor().orElse("SYSTEM"));
                    dto.setLastModifiedDate(LocalDateTime.now());
                    dto.setFieldInfos(Arrays.stream(fields).map(field -> {
                        DataChangeLogDTO.FieldInfo fieldInfo = new DataChangeLogDTO.FieldInfo();
                        fieldInfo.setField(field.getName());
                        fieldInfo.setNewValue(ReflectUtils.getFieldValue(newResult, field));
                        fieldInfo.setOldValue(ReflectUtils.getFieldValue(oldResult, field));
                        return fieldInfo;
                    }).toList());
                    applicationEventPublisher.publishEvent(new DataChangeLogEvent(this, dto));
                }
            }
        }

        return proceed;
    }
}
