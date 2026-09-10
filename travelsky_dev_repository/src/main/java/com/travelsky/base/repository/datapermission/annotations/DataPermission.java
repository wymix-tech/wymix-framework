package com.travelsky.base.repository.datapermission.annotations;

import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataPermission {

    String[] statementIds() default "ALL";

    Class<? extends PermissionSqlInjector>[] injectors();

}
