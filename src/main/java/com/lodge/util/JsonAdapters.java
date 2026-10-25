package com.lodge.util;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gson adapters for java.time types (Gson has no built-in support for them).
 */
public final class JsonAdapters {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JsonAdapters() {
    }

    public static Type localDateType() {
        return new TypeToken<LocalDate>() { }.getType();
    }

    public static Type localDateTimeType() {
        return new TypeToken<LocalDateTime>() { }.getType();
    }

    public static JsonSerializer<LocalDate> localDateSerializer() {
        return (src, type, ctx) -> new JsonPrimitive(src.format(DATE));
    }

    public static JsonDeserializer<LocalDate> localDateDeserializer() {
        return (json, type, ctx) -> LocalDate.parse(json.getAsString(), DATE);
    }

    public static JsonSerializer<LocalDateTime> localDateTimeSerializer() {
        return (src, type, ctx) -> new JsonPrimitive(src.format(DATE_TIME));
    }

    public static JsonDeserializer<LocalDateTime> localDateTimeDeserializer() {
        return (json, type, ctx) -> LocalDateTime.parse(json.getAsString(), DATE_TIME);
    }
}
