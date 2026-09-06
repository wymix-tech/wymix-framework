package com.travelsky.base.repository.audit.supports;


import com.travelsky.base.repository.jpa.audit.domain.AbstractAuditEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;

import java.time.LocalDateTime;

public class AuditHandler {

    @Autowired
    private AuditorAware<String> auditorAware;

    public void touchForCreate(Object target) {
        if (!isAuditable(target)) {
            return;
        }
        AbstractAuditEntity audit = (AbstractAuditEntity) target;
        audit.setCreatedBy(auditorAware.getCurrentAuditor().orElse("SYSTEM"));
        audit.setCreatedTime(LocalDateTime.now());
    }


    public void touchForUpdate(Object target) {
        if (!isAuditable(target)) {
            return;
        }
        AbstractAuditEntity audit = (AbstractAuditEntity) target;
        audit.setLastModifiedBy(auditorAware.getCurrentAuditor().orElse("SYSTEM"));
        audit.setLastModifiedTime(LocalDateTime.now());
    }

    private boolean isAuditable(Object target) {
        return target instanceof AbstractAuditEntity;
    }


}
