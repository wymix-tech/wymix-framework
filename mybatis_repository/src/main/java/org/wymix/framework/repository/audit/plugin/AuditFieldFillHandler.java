package org.wymix.framework.repository.audit.plugin;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.wymix.framework.common.context.Principal;
import org.wymix.framework.common.context.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Optional;

public class AuditFieldFillHandler implements MetaObjectHandler {
    private static final Principal<Object> principal;

    static {
        principal = new Principal<>();
        principal.setPrincipal("SYSTEM");
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        this.setFieldValByName("createdBy", getAuthor(), metaObject);
        this.setFieldValByName("createdTime", LocalDateTime.now(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("lastModifiedBy", getAuthor(), metaObject);
        this.setFieldValByName("lastModifiedTime", LocalDateTime.now(), metaObject);
    }

    private String getAuthor() {
        return Optional.ofNullable(UserContextHolder.getUserContext()).orElse(principal).getPrincipal();
    }
}
