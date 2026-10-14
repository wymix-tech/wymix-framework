package org.wymix.framework.repository.datapermission.annotations;

import org.wymix.framework.repository.datapermission.registry.DataPermissionInjectorRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import({DataPermissionInjectorRegistry.class})
public @interface EnableDataPermission {

    String[] basePackages() default "";

}
