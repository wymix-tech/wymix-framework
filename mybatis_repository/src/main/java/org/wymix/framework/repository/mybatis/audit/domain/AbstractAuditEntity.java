package org.wymix.framework.repository.mybatis.audit.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AbstractAuditEntity implements Serializable {

    @TableField(value = "CREATED_BY", fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(value = "CREATED_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "LAST_MODIFIED_BY", fill = FieldFill.UPDATE)
    private String lastModifiedBy;

    @TableField(value = "LAST_MODIFIED_TIME", fill = FieldFill.UPDATE)
    private LocalDateTime lastModifiedTime;

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
