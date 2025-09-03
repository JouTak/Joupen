package org.joupen.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.joupen.repository.CustomLocalDateTimeDeserializer;
import org.joupen.repository.CustomLocalDateTimeSerializer;

import java.time.LocalDateTime;

public class Utils {
    public static ObjectMapper mapper;

    {
        mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
        module.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());

        mapper.registerModule(module);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
}
