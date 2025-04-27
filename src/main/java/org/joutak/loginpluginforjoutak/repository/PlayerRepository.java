package org.joutak.loginpluginforjoutak.repository;

import jakarta.persistence.EntityManager;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;

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
    EntityManager getEntityManager(); // Новый метод
}