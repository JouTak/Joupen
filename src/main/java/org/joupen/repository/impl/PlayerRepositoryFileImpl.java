package org.joupen.repository.impl;

import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.joupen.domain.PlayerEntity;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.JoupenProperties;
import org.joupen.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
public class PlayerRepositoryFileImpl implements PlayerRepository {

    public PlayerRepositoryFileImpl() {
        ensurePlayersFileExists();
    }

    @Override
    public Optional<PlayerEntity> findByUuid(UUID uuid) {
        return readPlayerDtos().stream()
                .filter(dto -> dto.getUuid().equals(uuid))
                .findFirst();
    }

    @Override
    public Optional<PlayerEntity> findByName(String name) {
        return readPlayerDtos().stream()
                .filter(dto -> dto.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<PlayerEntity> findAll() {
        return readPlayerDtos();
    }

    @Override
    public void save(PlayerEntity entity) {
        List<PlayerEntity> playerList = readPlayerDtos();
        playerList.add(entity);
        writePlayerDtos(playerList);
    }

    @Override
    public void updateByUuid(PlayerEntity playerDto, UUID uuid) {
        List<PlayerEntity> playerList = readPlayerDtos();
        for (int i = 0; i < playerList.size(); i++) {
            if (playerList.get(i).getUuid().equals(uuid)) {
                playerList.set(i, playerDto);
                writePlayerDtos(playerList);
                return;
            }
        }
    }

    @Override
    public void updateByName(PlayerEntity entity, String name) {
        List<PlayerEntity> playerDtos = readPlayerDtos();
        for (int i = 0; i < playerDtos.size(); i++) {
            if (playerDtos.get(i).getName().equalsIgnoreCase(name)) {
                playerDtos.set(i, entity);
                writePlayerDtos(playerDtos);
                return;
            }
        }
    }

    @Override
    public void delete(UUID uuid) {
        List<PlayerEntity> playerDtos = readPlayerDtos();
        playerDtos.removeIf(dto -> dto.getUuid().equals(uuid));
        writePlayerDtos(playerDtos);
    }

    private List<PlayerEntity> readPlayerDtos() {
        ensurePlayersFileExists();

        File jsonFile = new File(JoupenProperties.playersFilepath);
        log.info("Reading from file: {}", jsonFile.getAbsolutePath());

        try {
            String fileContent = Files.readString(jsonFile.toPath());
            if (fileContent == null || fileContent.isBlank()) {
                return new ArrayList<>();
            }

            Type listType = new TypeToken<List<PlayerEntity>>() {
            }.getType();
            List<PlayerEntity> players = Utils.fromJson(fileContent, listType);
            return players != null ? players : new ArrayList<>();
        } catch (IOException e) {
            log.error("Error reading players file", e);
            return new ArrayList<>();
        }
    }

    private void writePlayerDtos(List<PlayerEntity> players) {
        ensurePlayersFileExists();

        File jsonFile = new File(JoupenProperties.playersFilepath);
        try {
            Files.writeString(jsonFile.toPath(), Utils.toJson(players));
        } catch (IOException e) {
            log.error("Error writing players file", e);
        }
    }

    private void ensurePlayersFileExists() {
        File jsonFile = new File(JoupenProperties.playersFilepath);
        File parentDirectory = jsonFile.getParentFile();

        try {
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory.toPath());
            }
            if (!jsonFile.exists()) {
                Files.writeString(jsonFile.toPath(), "[]");
            }
        } catch (IOException e) {
            log.error("Error ensuring players file exists", e);
        }
    }
}
