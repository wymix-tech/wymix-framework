package org.wymix.framework.repository.datachangelog.supports;

import org.wymix.framework.repository.datachangelog.annotation.DataChangeLog;
import org.wymix.framework.repository.datachangelog.dto.DataChangeLogDTO;
import org.wymix.framework.repository.datachangelog.enums.StatementType;
import org.wymix.framework.repository.datachangelog.event.DataChangeLogEvent;
import org.wymix.framework.repository.datamigration.annotations.DataMigration;
import org.wymix.framework.utils.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PreUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.util.ReflectionUtils;

import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

public class DataChangeLogHandler {

    private static final Logger log = LoggerFactory.getLogger(DataChangeLogHandler.class);


    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private AuditorAware<String> auditorAware;

    private static Object getOldFieldValue(String propertyName, Object oldObject, Class<?> clz) {
        Field field = ReflectionUtils.findField(clz, propertyName);
        ReflectionUtils.makeAccessible(field);
        Object value = ReflectionUtils.getField(field, oldObject);
        if ((!Modifier.isPublic(field.getModifiers()) ||
                !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
                Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(false);
        }
        return value;
    }

    public void touchForDelete(PostDeleteEvent event) {
        if (!needRecord(event.getEntity().getClass(), StatementType.REMOVE)) {
            return;
        }
        DataChangeLogDTO dto = new DataChangeLogDTO();
        dto.setStatementType(StatementType.REMOVE);
        dto.setId(event.getId());
        String[] propertyNames = event.getPersister().getEntityMetamodel().getPropertyNames();
        Object[] oldValues = event.getDeletedState();

        for (int i = 0; i < propertyNames.length; i++) {
            if (filter(propertyNames[i]))
                dto.getFieldInfos().add(new DataChangeLogDTO.FieldInfo(propertyNames[i], oldValues[i], null));
        }

        dto.setLastModifiedBy(auditorAware.getCurrentAuditor().orElse("SYSTEM"));
        dto.setLastModifiedDate(LocalDateTime.now());
        dto.setClassName(event.getEntity().getClass().getName());
        dto.setTableName(event.getEntity().getClass().getAnnotation(Table.class).name());
        dto.setUri(DataChangeLogListener.getUri());
        dto.setRequestId(DataChangeLogListener.getRequestId());
        dto.setRequestRemoteIp(DataChangeLogListener.getRequestRemoteIp());
        applicationEventPublisher.publishEvent(new DataChangeLogEvent(this, dto));
    }

    public void touchForInsert(PostInsertEvent event) {
        if (!needRecord(event.getEntity().getClass(), StatementType.INSERT)) {
            return;
        }
        DataChangeLogDTO dto = new DataChangeLogDTO();
        dto.setStatementType(StatementType.INSERT);
        dto.setId(event.getId());
        String[] propertyNames = event.getPersister().getEntityMetamodel().getPropertyNames();
        Object[] newValues = event.getState();

        for (int i = 0; i < propertyNames.length; i++) {
            if (filter(propertyNames[i]))
                dto.getFieldInfos().add(new DataChangeLogDTO.FieldInfo(propertyNames[i], null, newValues[i]));
        }

        dto.setLastModifiedBy(auditorAware.getCurrentAuditor().orElse("SYSTEM"));
        dto.setLastModifiedDate(LocalDateTime.now());
        dto.setClassName(event.getEntity().getClass().getName());
        dto.setTableName(event.getEntity().getClass().getAnnotation(Table.class).name());
        dto.setUri(DataChangeLogListener.getUri());
        dto.setRequestId(DataChangeLogListener.getRequestId());
        dto.setRequestRemoteIp(DataChangeLogListener.getRequestRemoteIp());
        applicationEventPublisher.publishEvent(new DataChangeLogEvent(this, dto));
    }

    public void touchForUpdate(PreUpdateEvent event, Object oldObject) {
        if (!needRecord(event.getEntity().getClass(), StatementType.MODIFY)) {
            return;
        }
        DataChangeLogDTO dto = new DataChangeLogDTO();
        dto.setStatementType(StatementType.MODIFY);
        dto.setId(event.getId());
        String[] propertyNames = event.getPersister().getEntityMetamodel().getPropertyNames();
        Object[] newValues = event.getState();
        String propertyName;
        Object oldValue;
        Object newValue;
        for (int i = 0; i < propertyNames.length; i++) {
            propertyName = propertyNames[i];
            oldValue = getOldFieldValue(propertyName, oldObject, event.getEntity().getClass());
            newValue = newValues[i];
            if (filter(propertyName)) {
                dto.getFieldInfos().add(new DataChangeLogDTO.FieldInfo(propertyName, oldValue, newValue));
            }
        }

        dto.setLastModifiedBy(auditorAware.getCurrentAuditor().orElse("SYSTEM"));
        dto.setLastModifiedDate(LocalDateTime.now());
        dto.setClassName(event.getEntity().getClass().getName());
        dto.setTableName(event.getEntity().getClass().getAnnotation(Table.class).name());
        dto.setUri(DataChangeLogListener.getUri());
        dto.setRequestId(DataChangeLogListener.getRequestId());
        dto.setRequestRemoteIp(DataChangeLogListener.getRequestRemoteIp());
        applicationEventPublisher.publishEvent(new DataChangeLogEvent(this, dto));
    }

    private boolean filter(String name) {
        return !StringUtils.equalsAny(name, "createdBy", "createdTime", "lastModifiedBy", "lastModifiedTime");
    }

    private boolean needRecord(Class<?> clazz, StatementType statementType) {
        return (clazz.isAnnotationPresent(DataChangeLog.class) && ArrayUtils.contains(clazz.getAnnotation(DataChangeLog.class).record(), statementType)) || clazz.isAnnotationPresent(DataMigration.class);
    }
}
