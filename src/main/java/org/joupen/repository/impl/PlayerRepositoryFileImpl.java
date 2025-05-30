package org.joupen.repository.impl;

import jakarta.persistence.EntityManager;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.dto.PlayerDtos;
import org.joupen.inputoutput.JsonReaderImpl;
import org.joupen.inputoutput.JsonWriterImpl;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.JoupenProperties;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerRepositoryFileImpl implements PlayerRepository {

    private final PlayerMapper playerMapper;

    public PlayerRepositoryFileImpl() {
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
    }

    @Override
    public Optional<PlayerEntity> findByUuid(UUID uuid) {
        PlayerDtos playerDtos = readPlayerDtos();
        if (playerDtos == null || playerDtos.getPlayerDtoList() == null) {
            return Optional.empty();
        }
        return playerDtos.getPlayerDtoList().stream()
                .filter(dto -> dto.getUuid().equals(uuid))
                .findFirst()
                .map(playerMapper::dtoToEntity);
    }

    @Override
    public Optional<PlayerEntity> findByName(String name) {
        PlayerDtos playerDtos = readPlayerDtos();
        if (playerDtos == null || playerDtos.getPlayerDtoList() == null) {
            return Optional.empty();
        }
        return playerDtos.getPlayerDtoList().stream()
                .filter(dto -> dto.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(playerMapper::dtoToEntity);
    }

    @Override
    public List<PlayerEntity> findAll() {
        PlayerDtos playerDtos = readPlayerDtos();
        if (playerDtos == null || playerDtos.getPlayerDtoList() == null) {
            return new ArrayList<>();
        }
        return playerDtos.getPlayerDtoList().stream()
                .map(playerMapper::dtoToEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void save(PlayerDto playerDto) {
        PlayerDtos playerDtos = readPlayerDtos();
        if (playerDtos == null) {
            playerDtos = new PlayerDtos();
            playerDtos.setPlayerDtoList(new ArrayList<>());
        }
        playerDtos.getPlayerDtoList().add(playerDto);
        writePlayerDtos(playerDtos);
    }

    @Override
    public void update(PlayerDto playerDto) {
        PlayerDtos playerDtos = readPlayerDtos();
        if (playerDtos != null && playerDtos.getPlayerDtoList() != null) {
            List<PlayerDto> list = playerDtos.getPlayerDtoList();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getUuid().equals(playerDto.getUuid())) {
                    list.set(i, playerDto);
                    writePlayerDtos(playerDtos);
                    return;
                }
            }
        }
    }

    @Override
    public EntityManager getEntityManager() {
        return null;
    }

    @Override
    public void delete(UUID uuid) {
        PlayerDtos playerDtos = readPlayerDtos();
        if (playerDtos != null && playerDtos.getPlayerDtoList() != null) {
            playerDtos.getPlayerDtoList().removeIf(dto -> dto.getUuid().equals(uuid));
            writePlayerDtos(playerDtos);
        }
    }

    private PlayerDtos readPlayerDtos() {
        JsonReaderImpl reader = new JsonReaderImpl(JoupenProperties.playersFilepath);
        return reader.read();
    }

    private void writePlayerDtos(PlayerDtos playerDtos) {
        JsonWriterImpl writer = new JsonWriterImpl(JoupenProperties.playersFilepath);
        writer.write(playerDtos);
    }
}