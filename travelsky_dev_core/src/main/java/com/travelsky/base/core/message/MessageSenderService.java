package com.travelsky.base.core.message;

import com.travelsky.base.core.dto.Message;

public interface MessageSenderService {

    Message.SendType type();

    void send(Message message);

}
