package org.joutak.loginpluginforjoutak.inputoutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.dto.PlayerDtos;
import org.joutak.loginpluginforjoutak.utils.JoutakProperties;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class JsonWriterImpl implements Writer {

    private final String filepath;
    private final ObjectMapper objectMapper;

    public JsonWriterImpl(String filepath) {
        this.filepath = filepath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void write(PlayerDtos playerDtos) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
            // Используем pretty printer для форматированного вывода
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(playerDtos);
            writer.write(json);
            log.info("Json save was completed successfully");
        } catch (IOException e) {
            log.error("Can't write to file: {}", filepath, e);
        }
    }

    @Override
    public void addNew(PlayerDto playerDto) {
        Reader reader = new JsonReaderImpl(JoutakProperties.saveFilepath);
        PlayerDtos playerDtos = reader.read();
        playerDtos.getPlayerDtoList().add(playerDto);
        write(playerDtos);
    }
}