package org.wymix.framework.core.message;

import org.wymix.framework.core.dto.Message;

public interface MessageSenderService {

    Message.SendType type();

    void send(Message message);

}
