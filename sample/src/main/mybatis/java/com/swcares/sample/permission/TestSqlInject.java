package com.swcares.sample.permission;

import com.travelsky.base.repository.datapermission.enums.InjectType;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;

import java.util.Collections;
import java.util.Map;

/**
 * 数据权限 SQL 注入示例。
 *
 * <p>位于 {@code @EnableDataPermission(basePackages = ...)} 指定的扫描包下即可，
 * <b>无需</b>标注 {@code @Component}，注册器会自动扫描 {@link PermissionSqlInjector} 的实现并装配。
 *
 * <p>生效范围由方法上的 {@code @DataPermission(injectors = ...)} 决定，
 * 未标注的方法不会追加任何条件。
 */
public class TestSqlInject implements PermissionSqlInjector {

    /**
     * 附加参数：配合 injectSql 中的 {@code :xxx} / #{xxx} 占位符使用，此处无需参数。
     */
    @Override
    public Map<String, Object> additionalParameters() {
        return Collections.emptyMap();
    }

    /**
     * 追加的过滤条件，会被拼接到原 SQL 的 WHERE 之后。
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
