package com.travelsky.base.repository.datachangelog.dto;

import com.travelsky.base.repository.datachangelog.enums.StatementType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DataChangeLogDTO {

    private StatementType statementType;
    private Object id;
    private List<FieldInfo> fieldInfos = new ArrayList<>();
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String className;
    private String tableName;
    private String uri;
    private Long requestId;
    private String requestRemoteIp;

    public StatementType getStatementType() {
        return statementType;
    }

    public void setStatementType(StatementType statementType) {
        this.statementType = statementType;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public List<FieldInfo> getFieldInfos() {
        return fieldInfos;
    }

    public void setFieldInfos(List<FieldInfo> fieldInfos) {
        this.fieldInfos = fieldInfos;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getRequestRemoteIp() {
        return requestRemoteIp;
    }

    public void setRequestRemoteIp(String requestRemoteIp) {
        this.requestRemoteIp = requestRemoteIp;
    }

    public static class FieldInfo {
        private String field;

        private Object oldValue;

        private Object newValue;

        public FieldInfo() {

        }

        public FieldInfo(String field, Object oldValue, Object newValue) {
            this.field = field;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public void setNewValue(Object newValue) {
            this.newValue = newValue;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public void setOldValue(Object oldValue) {
            this.oldValue = oldValue;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }
}
