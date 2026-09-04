package com.travelsky.base.repository.audit.supports;

import com.travelsky.base.core.context.ApplicationContextHolder;
import org.springframework.beans.factory.aspectj.ConfigurableObject;
import org.springframework.util.Assert;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class AuditListener implements ConfigurableObject {


    private AuditHandler auditHandler;

    private AuditHandler getAuditHandler() {
        if (auditHandler == null) {
            synchronized (this) {
                if (auditHandler == null) {
                    auditHandler = ApplicationContextHolder.getApplicationContext().getBean(AuditHandler.class);
                }
            }
        }
        return auditHandler;
    }

    @PrePersist
    private void touchForCreate(Object target) {
        Assert.notNull(target, "Entity must not be null!");
        getAuditHandler().touchForCreate(target);
    }

    @PreUpdate
    private void touchForUpdate(Object target) {
        Assert.notNull(target, "Entity must not be null!");
        getAuditHandler().touchForUpdate(target);
    }

}
