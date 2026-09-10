package com.travelsky.base.repository.configuration;

import com.travelsky.base.repository.audit.supports.AuditHandler;
import com.travelsky.base.repository.audit.supports.AuditListener;
import com.travelsky.base.repository.audit.supports.DatabaseAuditorAware;
import com.travelsky.base.repository.datapermission.aop.DataPermissionAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(prefix = "travelsky.jpa", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(AuditListener.class)
@Configuration
@DependsOn("entityManagerFactory")
public class JpaConfiguration {


    @Bean
    public DatabaseAuditorAware auditorAware() {
        return new DatabaseAuditorAware();
    }

    @Bean
    public AuditHandler auditHandler() {
        return new AuditHandler();
    }

    /**
     * 数据权限切面：JPA 侧消费 {@code @DataPermission} 的入口，
     * 由 {@code SpecificationHelper#applyDataPermission} 在查询时读取上下文追加 {@code Predicate}。
     *
     * <p>MyBatis 场景 {@code @EnableDataPermission} 也会注册同类型切面，此处 {@code @ConditionalOnMissingBean} 保证不重复注册。
     */
    @Bean
    @ConditionalOnMissingBean
    public DataPermissionAspect dataPermissionAspect() {
        return new DataPermissionAspect();
    }


}
