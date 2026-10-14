package org.wymix.framework.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.wymix.framework.utils.DateUtils;
import org.wymix.framework.utils.StringUtils;

import java.io.IOException;
import java.util.Date;

public class DateDeserializer extends JsonDeserializer<Date> {

    public static String FORMATTER = "yyyy-MM-dd HH:mm:ss";
    public static String FORMATTER_1 = "yyyyMMddHHmmss";
    public static String ONLY_DATE_FORMATTER = "yyyy-MM-dd";
    public static String ONLY_DATE_FORMATTER_2 = "yyyyMMdd";

    @Override
    public Date deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        if (StringUtils.isEmpty(jp.getText())) {
            return null;
        }
        Date date = DateUtils.parse(jp.getText());
        if (date != null) {
            return date;
        }
        throw new RuntimeException("Date deserialize error！");
    }
}