package org.wymix.framework.core.configuration;

import org.wymix.framework.core.context.ApplicationContextHolder;
import org.wymix.framework.core.message.EmailSenderService;
import org.wymix.framework.core.message.MessageSenderService;
import org.wymix.framework.core.message.MessageService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@EnableConfigurationProperties(AsyncScheduleProperties.class)
@ConditionalOnProperty(prefix = "wymix.core", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CoreConfiguration {

    @Bean
    public ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }

    @Bean
    @ConditionalOnBean(MessageSenderService.class)
    public MessageService messageService() {
        return new MessageService() {
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    public EmailSenderService emailSenderService() {
        return new EmailSenderService();
    }

}
