package org.wymix.framework.repository.configuration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.wymix.framework.repository.audit.plugin.AuditFieldFillHandler;
import org.wymix.framework.utils.GlobalIdGenerator;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.type.JdbcType;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ConditionalOnProperty(prefix = "wymix.mybatis", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class MyBatisConfiguration {

    @Bean
    public Interceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        paginationInnerInterceptor.setMaxLimit(10000L);
        mybatisPlusInterceptor.addInnerInterceptor(paginationInnerInterceptor);
        return mybatisPlusInterceptor;
    }

    @Bean
    public MetaObjectHandler auditFieldFillHandler() {
        return new AuditFieldFillHandler();
    }

    private IdentifierGenerator getCustomIdGenerator() {
        return entity -> GlobalIdGenerator.generateNextId();
    }

    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer() {
        return properties -> {
            MybatisPlusProperties.CoreConfiguration configuration = properties.getConfiguration();
            if (configuration == null) {
                configuration = new MybatisPlusProperties.CoreConfiguration();
                properties.setConfiguration(configuration);
            }
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setJdbcTypeForNull(JdbcType.NULL);

            properties.getGlobalConfig()
                    .setIdentifierGenerator(getCustomIdGenerator())
                    .getDbConfig()
                    .setIdType(IdType.INPUT)
            ;
        };
    }


}
