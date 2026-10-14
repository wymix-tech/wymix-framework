package org.wymix.framework.security.customizer;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@FunctionalInterface
public interface SecurityCustomizer {


    void customize(final HttpSecurity httpSecurity);

}
