package com.swcares.sample.permission;

import org.wymix.framework.repository.datapermission.enums.InjectType;
import org.wymix.framework.repository.datapermission.inject.PermissionSqlInjector;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 数据权限 SQL 注入示例（JPA 版）。
 *
 * <p><b>与 MyBatis 版的关键差异</b>：JPA 场景不使用 {@code @EnableDataPermission}，
 * 没有注册器会自动扫描 {@link PermissionSqlInjector} 的实现，因此这里<b>必须</b>标注
 * {@link Component}（或在配置类中声明 {@code @Bean}），否则 {@code SpecificationHelper} 取注入器时
 * 会抛「未注册为 Spring Bean」的异常。MyBatis 版无需 {@code @Component} 是因为
 * {@code DataPermissionInjectorRegistry} 会扫描并注册。
 *
 * <p>{@code injectSql()} 写数据库列名（{@code CREATED_BY}）或实体属性名（{@code createdBy}）均可，
 * JPA 侧会先按属性名解析，失败再按 {@code @Column} / 下划线命名策略反查。
 *
 * <p>生效范围由方法上的 {@code @DataPermission(injectors = ...)} 决定，
 * 未标注的方法不会追加任何条件。
 */
@Component
public class TestSqlInject implements PermissionSqlInjector {

    /**
     * 附加参数：配合 injectSql 中的 {@code :xxx} / #{xxx} 占位符使用，此处无需参数。
     */
    @Override
    public Map<String, Object> additionalParameters() {
        return Collections.emptyMap();
    }

    /**
     * 追加的过滤条件，由 JPA 侧翻译为 {@code Predicate} 追加到查询条件上。
     */
    @Override
    public String injectSql() {
        return "CREATED_BY = 'SYSTEM'";
    }

    @Override
    public InjectType type() {
        /*
        InjectType 分为两种
        1. REPLACE_TABLE_NAME  修改表名   此时可以在 injectSql() 方法中按照格式  A -> B  的方式 将执行的sql中的A表替换为B表
        2. WHERE_ADDITIONAL  附加where条件
         */
        return InjectType.WHERE_ADDITIONAL;
    }
}
