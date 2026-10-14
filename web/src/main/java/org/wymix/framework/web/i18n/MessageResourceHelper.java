package org.wymix.framework.web.i18n;

import org.wymix.framework.core.context.ApplicationContextHolder;
import org.springframework.context.MessageSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;
import java.util.TimeZone;

public class MessageResourceHelper {

    private MessageResourceHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * generateMessages
     *
     * @param globalMessage String
     * @return String
     */
    public static String generateMessages(String globalMessage) {
        return generateMessages(globalMessage, null);
    }

    /**
     * generateMessages
     *
     * @param globalMessage String
     * @param args          Object[]
     * @return String
     */
    public static String generateMessages(String globalMessage, Object[] args) {
        MessageSource messageSource = ApplicationContextHolder.getApplicationContext().getBean(MessageSource.class);
        AcceptHeaderLocaleResolver acceptHeaderLocaleResolver = ApplicationContextHolder
                .getApplicationContext().getBean(AcceptHeaderLocaleResolver.class);
        return messageSource.getMessage(globalMessage, args, globalMessage,
                acceptHeaderLocaleResolver.resolveLocale(
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                                .getRequest()));
    }

    public static Locale resolveLocale() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (sra == null) {
            return getDefaultLocale();
        }
        try {
            return ApplicationContextHolder.getApplicationContext()
                    .getBean(AcceptHeaderLocaleResolver.class).resolveLocale(
                            ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                                    .getRequest());
        } catch (Exception ex) {
            return getDefaultLocale();
        }
    }

    public static boolean isEnLanguage() {
        return Locale.ENGLISH.getDisplayLanguage().equals(resolveLocale().getDisplayLanguage());
    }

    public static Locale getDefaultLocale() {
        if (TimeZone.getDefault()
                .getRawOffset() == (TimeZone.getTimeZone("GMT+08").getRawOffset())) {
            return Locale.CHINA;
        }
        return Locale.getDefault();
    }

}
