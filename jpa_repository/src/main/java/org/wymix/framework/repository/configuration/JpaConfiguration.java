package org.wymix.framework.repository.configuration;

import org.wymix.framework.repository.audit.supports.AuditHandler;
import org.wymix.framework.repository.audit.supports.AuditListener;
import org.wymix.framework.repository.audit.supports.DatabaseAuditorAware;
import org.wymix.framework.repository.datapermission.aop.DataPermissionAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(prefix = "wymix.jpa", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(AuditListener.class)
@Configuration
@DependsOn("entityManagerFactory")
@Deprecated(since = "3.0", forRemoval = false)
public class JpaConfiguration {
    @jakarta.annotation.PostConstruct
    public void deprecatedModuleNotice() {
        org.slf4j.LoggerFactory.getLogger(JpaConfiguration.class).warn("jpa_repository is deprecated and maintained for compatibility only. New projects should use mybatis_repository; manual/batch DML audit is unsupported.");
    }



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
     * <p><b>为什么用属性开关而不是只靠 {@code @ConditionalOnMissingBean} 做去重</b>：
     * 本类同时标注了 {@code @AutoConfiguration} 与 {@code @Configuration}，其 {@code @Bean} 方法参与
     * 用户配置类的 Bean 定义注册，而 {@code @ConditionalOnMissingBean} 的判定顺序依赖配置类处理顺序，
     * 无法稳定屏蔽 {@code @EnableDataPermission} 注册器已注册的同类型切面 —— 两者同时注册时，
     * 内层切面的 {@code finally} 会提前清掉上下文（现切面已改为「恢复外层值」，但重复代理仍应尽量避免）。
     *
     * <p>因此这里改为显式开关（默认 {@code true}，纯 JPA 场景开箱可用）：
     * <ul>
     *   <li>不配置或配置 {@code true} —— 注册本切面，JPA 侧数据权限生效；</li>
     *   <li>配置 {@code false} —— 不注册；同时引入 MyBatis 侧 {@code @EnableDataPermission} 时
     *   由注册器统一注册（建议此时设为 {@code false}，只保留一个切面实例）；</li>
     *   <li>业务方自行声明 {@code DataPermissionAspect} Bean 时，{@code @ConditionalOnMissingBean} 让位。</li>
     * </ul>
     * 即便两种 ORM 同时引入导致双切面，切面内部已在 {@code finally} 中恢复外层上下文（而非直接 clear），
     * 不会出现「条件被提前清空、静默返回全量数据」的问题。
     * 本类只标注 {@code @Aspect}，AOP 代理由 core 的 {@code @EnableAsync(proxyTargetClass = true)} 间接开启；
     * 未引入 core 时需自行 {@code @EnableAspectJAutoProxy}。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "wymix.jpa.data-permission", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DataPermissionAspect dataPermissionAspect() {
        return new DataPermissionAspect();
    }


}
