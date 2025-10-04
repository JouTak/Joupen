package org.joupen.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.joupen.domain.PlayerEntity;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.JoupenProperties;
import org.mapstruct.factory.Mappers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.joupen.utils.Utils.mapper;

@Slf4j
public class PlayerRepositoryFileImpl implements PlayerRepository {
    private final PlayerMapper playerMapper;

    public PlayerRepositoryFileImpl() {
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
    }

    @Override
    public Optional<PlayerEntity> findByUuid(UUID uuid) {
        List<PlayerEntity> playerDtos = readPlayerDtos();
        if (playerDtos == null) {
            return Optional.empty();
        }
        return playerDtos.stream()
                .filter(dto -> dto.getUuid().equals(uuid))
                .findFirst();
    }

    @Override
    public Optional<PlayerEntity> findByName(String name) {
        List<PlayerEntity> playerDtos = readPlayerDtos();
        if (playerDtos == null) {
            return Optional.empty();
        }
        return playerDtos.stream()
                .filter(dto -> dto.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<PlayerEntity> findAll() {
        List<PlayerEntity> playerDtos = readPlayerDtos();
        if (playerDtos == null) {
            return new ArrayList<>();
        }
        return playerDtos;
    }

    @Override
    public void save(PlayerEntity entity) {
        List<PlayerEntity> playerList = readPlayerDtos();
        if (playerList == null) {
            playerList = new ArrayList<>();
        }
        playerList.add(entity);
        writePlayerDtos(playerList);
    }

    @Override
    public void updateByUuid(PlayerEntity playerDto, UUID uuid) {
        List<PlayerEntity> playerList = readPlayerDtos();
        if (playerList != null) {
            for (int i = 0; i < playerList.size(); i++) {
                if (playerList.get(i).getUuid().equals(uuid)) {
                    playerList.set(i, playerDto);
                    writePlayerDtos(playerList);
                    return;
                }
            }
        }
    }

    @Override
    public void updateByName(PlayerEntity entity, String name) {
        List<PlayerEntity> playerDtos = readPlayerDtos();
        if (playerDtos != null) {
            for (int i = 0; i < playerDtos.size(); i++) {
                if (playerDtos.get(i).getName().equalsIgnoreCase(name)) {
                    playerDtos.set(i, entity);
                    writePlayerDtos(playerDtos);
                    return;
                }
            }
        }
    }

    @Override
    public void delete(UUID uuid) {
        List<PlayerEntity> playerDtos = readPlayerDtos();
        if (playerDtos != null) {
            playerDtos.removeIf(dto -> dto.getUuid().equals(uuid));
            writePlayerDtos(playerDtos);
        }
    }


    private List<PlayerEntity> readPlayerDtos() {
        try {
            File jsonFile = new File(JoupenProperties.playersFilepath);
            log.info("Reading from file: {}", jsonFile.getAbsolutePath());
            return mapper.readValue(jsonFile, new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("Error reading players file", e);
            return null;
        }
    }

    private void writePlayerDtos(List<PlayerEntity> players) {
        try {
            File jsonFile = new File(JoupenProperties.playersFilepath);
            mapper.writeValue(jsonFile, players);
        } catch (IOException e) {
            log.error("Error writing players file", e);
        }
    }
}