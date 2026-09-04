package com.travelsky.base.core.configuration;

import com.travelsky.base.core.context.ApplicationContextHolder;
import com.travelsky.base.core.message.EmailSenderService;
import com.travelsky.base.core.message.MessageSenderService;
import com.travelsky.base.core.message.MessageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
