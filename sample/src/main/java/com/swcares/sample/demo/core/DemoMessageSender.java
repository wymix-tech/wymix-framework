package com.swcares.sample.demo.core;

import org.wymix.framework.core.dto.Message;
import org.wymix.framework.core.message.MessageSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * core 模块消息发送 SPI 示例。
 *
 * <p>实现 {@link MessageSenderService} 并注册为 Bean 后，core 的 {@code CoreConfiguration}
 * 会自动装配出 {@link org.wymix.framework.core.message.MessageService} 门面；
 * 门面发送时会按 {@link #type()} 把消息路由到匹配的发送器。
 */
@Component
public class DemoMessageSender implements MessageSenderService {

    private static final Logger log = LoggerFactory.getLogger(DemoMessageSender.class);

    @Override
    public Message.SendType type() {
        // O=邮件 / E / P=站内推送，示例使用 P
        return Message.SendType.P;
    }

    @Override
    public void send(Message message) {
        log.info("[demo] 发送消息 id={}, subject={}, to={}, sendTypes={}",
                message.getId(), message.getSubject(), message.getTo(), Arrays.toString(message.getSendTypes()));
    }
}
