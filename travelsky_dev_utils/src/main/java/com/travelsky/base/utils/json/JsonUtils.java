package com.travelsky.base.utils.json;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.travelsky.base.utils.DateUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static ObjectMapper defaultDateDbjectMapper = null;
    private static ObjectMapper typeObjectMapper = null;

    static {
        JavaTimeModule timeModule = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        objectMapper.registerModule(timeModule);
    }

    public static String entityToJson(Object entity) {
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.setPropertyNamingStrategy(new PropertyNamingStrategy());
        if (entity == null) {
            return "";
        }

        try {
            return objectMapper.writeValueAsString(entity);
        } catch (IOException e) {
            return "";
        }
    }

    public static <T> T jsonToEntity(Class<?> collectionClass, Class<?> elementClasses,
                                     String str) {
        if (str == null) {
            return null;
        }
        try {
            JavaType javaType = getCollectionType(collectionClass, elementClasses);
            return objectMapper.readValue(str, javaType);
        } catch (IOException e) {
            return null;
        }
    }

    public static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return objectMapper.getTypeFactory().constructParametricType(collectionClass,
                elementClasses);
    }

    public static <T> T jsonToEntity(Class<T> clazz, String str) {
        if (str == null) {
            return null;
        }
        try {
            T result = objectMapper.readValue(str, clazz);
            return result;
        } catch (IOException e) {
            return null;
        }
    }

    public static String getValue(String json, String propertyName) {
        try {
            return objectMapper.readTree(json).get(propertyName).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static synchronized ObjectMapper generateDefaultDateformatterObjectMapper() {
        if (defaultDateDbjectMapper != null) {
            return defaultDateDbjectMapper;
        }
        // 设置解析JSON工具类
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(Date.class, new DateSerializer(false,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", DateUtils.getDefaultLocale())));

        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, new NumberSerializer(Number.class));
        module.addSerializer(Number.class, new NumberSerializer(Number.class));
        module.addDeserializer(Date.class, new DateDeserializer());
        defaultDateDbjectMapper = new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
                .registerModule(javaTimeModule).registerModule(module)
                .setPropertyNamingStrategy(new PropertyNamingStrategy());
        return defaultDateDbjectMapper;
    }

    public static String entityToJsonWithDefaultDateformat(Object obj) {
        ObjectMapper mapper = generateDefaultDateformatterObjectMapper();
        try {
            return mapper.writeValueAsString(obj);
        } catch (IOException e) {
            return "";
        }
    }

    public static <T> T jsonToEntity(String str, Type type) {
        if (str == null) {
            return null;
        }
        if (type == String.class) {
            return (T) str;
        }
        try {
            JsonDeserialize.CustomTypeReference<T> ct = new JsonDeserialize.CustomTypeReference<T>();
            ct.setType(type);
            T result = generateTypeObjectMapper().readValue(str, ct);
            return result;
        } catch (IOException e) {
            return null;

        }
    }

    public static <T> T jsonToEntityWithCommonDate(Class<T> clazz, String str) {
        if (str == null) {
            return null;
        }
        try {
            T result = generateDefaultDateformatterObjectMapper().readValue(str, clazz);
            return result;
        } catch (IOException e) {
            return null;
        }
    }

    private static synchronized ObjectMapper generateTypeObjectMapper() {
        if (typeObjectMapper != null) {
            return typeObjectMapper;
        }
        // 设置解析JSON工具类
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(Date.class, new DateDeserializer());

        typeObjectMapper = new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
                .registerModule(javaTimeModule)
                .setPropertyNamingStrategy(new PropertyNamingStrategy());
        return typeObjectMapper;
    }

}
