package org.joupen.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class Utils {
    private static final Gson gson;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    static {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new com.google.gson.JsonPrimitive(src.format(DATE_FORMATTER)))
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), DATE_FORMATTER))
                .create();
    }

    public static String toJson(Object obj) {
        try {
            return gson.toJson(obj);
        } catch (Exception exception) {
            log.error("При парсе {} произошла ошибка{}", obj.getClass(), exception.getMessage());
            return null;
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            log.error("Ошибка при парсинге json {} в класс {}: {}", json, clazz, e.getMessage());
            return null;
        }
    }

    public static <T> T fromJson(String json, Type type) {
        try {
            return gson.fromJson(json, type);
        } catch (Exception e) {
            log.error("Ошибка при парсинге json {}: {}", json, e.getMessage());
            return null;
        }
    }

}

