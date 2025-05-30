package org.joupen.inputoutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.joupen.dto.PlayerDto;
import org.joupen.dto.PlayerDtos;
import org.joupen.utils.JoupenProperties;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class JsonWriterImpl implements Writer {

    private final String filepath;
    private final ObjectMapper objectMapper;

    public JsonWriterImpl(String filepath) {
        this.filepath = filepath != null ? filepath : JoupenProperties.playersFilepath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void write(PlayerDtos playerDtos) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(playerDtos);
            writer.write(json);
            log.info("Json save was completed successfully");
        } catch (IOException e) {
            log.error("Can't write to file: {}", filepath, e);
        }
    }

    @Override
    public void addNew(PlayerDto playerDto) {
        Reader reader = new JsonReaderImpl(JoupenProperties.playersFilepath);
        PlayerDtos playerDtos = reader.read();
        if (playerDtos == null || playerDtos.getPlayerDtoList() == null) {
            playerDtos = new PlayerDtos();
            playerDtos.setPlayerDtoList(new java.util.ArrayList<>());
        }
        playerDtos.getPlayerDtoList().add(playerDto);
        write(playerDtos);
    }
}