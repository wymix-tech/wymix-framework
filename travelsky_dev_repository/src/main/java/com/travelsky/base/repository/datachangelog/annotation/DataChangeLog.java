package com.travelsky.base.repository.datachangelog.annotation;

import com.travelsky.base.repository.datachangelog.enums.StatementType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataChangeLog {

    StatementType[] record() default {StatementType.INSERT, StatementType.MODIFY, StatementType.REMOVE};

}
