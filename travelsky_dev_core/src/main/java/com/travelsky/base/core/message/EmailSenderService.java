package com.travelsky.base.core.message;

import com.travelsky.base.core.dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

public class EmailSenderService implements MessageSenderService {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);

    @Value("${spring.mail.username}")
    private String sender;

    /**
     * the javaMailSender
     */
    @Autowired
    private JavaMailSender javaMailSender;

    @Override
    public Message.SendType type() {
        return Message.SendType.O;
    }

    @Override
    public void send(Message msg) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            //    sendMail方法暂未使用，白盒扫描暂时注释掉下行代码
            helper.setFrom(sender);
            helper.setTo(msg.getTo().stream().toArray(String[]::new));
            helper.setCc(msg.getCc().stream().toArray(String[]::new));
            helper.setSubject(msg.getSubject());
            helper.setText(msg.getContent(), true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            log.error(e.getMessage(), e);
        }
    }
}
