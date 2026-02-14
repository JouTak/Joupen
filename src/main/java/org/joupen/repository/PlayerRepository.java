package org.joupen.repository;

import org.joupen.domain.PlayerEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository {
    Optional<PlayerEntity> findByUuid(UUID uuid);

    Optional<PlayerEntity> findByName(String name);

    void save(PlayerEntity playerDto);

    List<PlayerEntity> findAll();

    void updateByUuid(PlayerEntity playerDto, UUID uuid);

    void delete(UUID uuid);

    void updateByName(PlayerEntity playerDto, String name);
}