package com.travelsky.base.repository.datachangelog.annotations;

import com.travelsky.base.repository.datachangelog.configuration.DataChangeLogConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(DataChangeLogConfiguration.class)
public @interface EnableDataChangeLog {
}
