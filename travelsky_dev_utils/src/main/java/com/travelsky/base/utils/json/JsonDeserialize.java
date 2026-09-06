package com.travelsky.base.utils.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class JsonDeserialize<T> implements Comparable<JsonDeserialize<T>> {

    private static final Logger log = LoggerFactory.getLogger(JsonDeserialize.class);

    protected Type _type;

    public JsonDeserialize() {
        Class clazz = getClass();
        while (clazz != null && (!(clazz.getGenericSuperclass() instanceof ParameterizedType))) {
            clazz = clazz.getSuperclass();
        }
        if (clazz != null) {
            _type = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
        } else {
            throw new IllegalArgumentException(
                    "Invalid argument, " + "argument's super class must specify type parameters");
        }
        ;

    }

    public T jsonToEntity(String str) {
        ObjectMapper objectMapper = new ObjectMapper();

        if (str == null) {
            return null;
        }

        try {
            CustomTypeReference<T> c = new CustomTypeReference<T>();
            c.setType(_type);
            T result = objectMapper.readValue(str, c);

            return result;

        } catch (IOException e) {

            log.warn("Failed to deserialize json: {}", str, e);
            return null;

        }
    }

    @Override
    public int compareTo(JsonDeserialize<T> o) {
        // just need an implementation, not a good one... hence:
        return 0;
    }

    public static class CustomTypeReference<T> extends TypeReference<T> {

        private Type type;

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

    }
}