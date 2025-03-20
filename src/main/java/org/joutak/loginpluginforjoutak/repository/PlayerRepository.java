package org.joutak.loginpluginforjoutak.repository;

import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository {
    Optional<PlayerEntity> findByUuid(UUID uuid);

    List<PlayerEntity> findAll();

    void save(PlayerDto playerDto);

    void update(PlayerDto playerDto);

    void delete(UUID uuid);

    Optional<PlayerEntity> findByName(String name);
}

