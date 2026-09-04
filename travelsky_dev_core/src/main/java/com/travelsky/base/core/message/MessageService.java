package com.travelsky.base.core.message;

import com.travelsky.base.core.context.ApplicationContextHolder;
import com.travelsky.base.core.dto.Message;
import com.travelsky.base.utils.GlobalIdGenerator;

import java.util.Arrays;
import java.util.Collection;

public interface MessageService {

    default Message of(Message.SendType... sendTypes) {
        Message message = new Message();
        message.setSendTypes(sendTypes);
        message.setId(GlobalIdGenerator.generateNextId() + "");
        return message;
    }

    default void send(Message message) {
        Collection<MessageSenderService> senders = ApplicationContextHolder.getBeansOfType(MessageSenderService.class).values();
        senders.stream()
                .filter(service -> Arrays.asList(message.getSendTypes()).contains(service.type()))
                .forEach(service -> service.send(message));

    }

}
