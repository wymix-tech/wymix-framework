package org.wymix.framework.core.message;

import org.wymix.framework.core.context.ApplicationContextHolder;
import org.wymix.framework.core.dto.Message;
import org.wymix.framework.utils.GlobalIdGenerator;

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
