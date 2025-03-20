package org.joutak.loginpluginforjoutak.inputoutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.joutak.loginpluginforjoutak.dto.PlayerDtos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonReaderImpl implements Reader {

    private final String filepath;
    private final ObjectMapper objectMapper;

    public JsonReaderImpl(String filepath) {
        this.filepath = filepath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public PlayerDtos read() {
        try {
            String json = Files.readString(Paths.get(filepath));
            return objectMapper.readValue(json, PlayerDtos.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + filepath, e);
        }
    }
}