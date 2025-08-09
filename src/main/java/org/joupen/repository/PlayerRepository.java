package org.joupen.repository;

import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository {
    Optional<PlayerEntity> findByUuid(UUID uuid);
    Optional<PlayerEntity> findByName(String name);
    void save(PlayerDto playerDto);
    List<PlayerEntity> findAll();
    void delete(UUID uuid);
    void update(PlayerDto playerDto);
}