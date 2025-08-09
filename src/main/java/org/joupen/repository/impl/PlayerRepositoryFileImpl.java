package org.joupen.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.CustomLocalDateTimeDeserializer;
import org.joupen.repository.CustomLocalDateTimeSerializer;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.JoupenProperties;
import org.mapstruct.factory.Mappers;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class PlayerRepositoryFileImpl implements PlayerRepository {
    private final ObjectMapper mapper;
    private final PlayerMapper playerMapper;

    public PlayerRepositoryFileImpl() {
        this.mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
        module.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());
        this.mapper.registerModule(module);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
    }

    @Override
    public Optional<PlayerEntity> findByUuid(UUID uuid) {
        List<PlayerDto> playerDtos = readPlayerDtos();
        if (playerDtos == null) {
            return Optional.empty();
        }
        return playerDtos.stream()
                .filter(dto -> dto.getUuid().equals(uuid))
                .findFirst()
                .map(playerMapper::dtoToEntity);
    }

    @Override
    public Optional<PlayerEntity> findByName(String name) {
        List<PlayerDto> playerDtos = readPlayerDtos();
        if (playerDtos == null) {
            return Optional.empty();
        }
        return playerDtos.stream()
                .filter(dto -> dto.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(playerMapper::dtoToEntity);
    }

    @Override
    public List<PlayerEntity> findAll() {
        List<PlayerDto> playerDtos = readPlayerDtos();
        if (playerDtos == null) {
            return new ArrayList<>();
        }
        return playerDtos.stream()
                .map(playerMapper::dtoToEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void save(PlayerDto playerDto) {
        List<PlayerDto> playerDtos = readPlayerDtos();
        if (playerDtos == null) {
            playerDtos = new ArrayList<>();
        }
        playerDtos.add(playerDto);
        writePlayerDtos(playerDtos);
    }

    @Override
    public void update(PlayerDto playerDto) {
        List<PlayerDto> playerDtos = readPlayerDtos();
        if (playerDtos != null) {
            for (int i = 0; i < playerDtos.size(); i++) {
                if (playerDtos.get(i).getUuid().equals(playerDto.getUuid())) {
                    playerDtos.set(i, playerDto);
                    writePlayerDtos(playerDtos);
                    return;
                }
            }
        }
    }

    @Override
    public void delete(UUID uuid) {
        List<PlayerDto> playerDtos = readPlayerDtos();
        if (playerDtos != null) {
            playerDtos.removeIf(dto -> dto.getUuid().equals(uuid));
            writePlayerDtos(playerDtos);
        }
    }

    private List<PlayerDto> readPlayerDtos() {
        try {
            File jsonFile = new File(JoupenProperties.playersFilepath);
            log.info("Reading from file: {}", jsonFile.getAbsolutePath());
            return mapper.readValue(jsonFile, new TypeReference<List<PlayerDto>>() {
            });
        } catch (IOException e) {
            log.error("Error reading players file", e);
            return null;
        }
    }

    private void writePlayerDtos(List<PlayerDto> playerDtos) {
        try {
            File jsonFile = new File(JoupenProperties.playersFilepath);
            mapper.writeValue(jsonFile, playerDtos);
        } catch (IOException e) {
            log.error("Error writing players file", e);
        }
    }
}