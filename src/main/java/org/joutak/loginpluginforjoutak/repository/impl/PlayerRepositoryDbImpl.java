package org.joutak.loginpluginforjoutak.repository.impl;

import jakarta.persistence.EntityManager;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.mapper.PlayerMapper;
import org.joutak.loginpluginforjoutak.repository.PlayerRepository;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerRepositoryDbImpl implements PlayerRepository {

    private final EntityManager em;
    private final PlayerMapper playerMapper;

    public PlayerRepositoryDbImpl(EntityManager entityManager) {
        if (entityManager == null) {
            throw new IllegalArgumentException("EntityManager cannot be null");
        }
        this.em = entityManager;
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
    }

    @Override
    public Optional<PlayerEntity> findByUuid(UUID uuid) {
        try {
            return Optional.ofNullable(em.find(PlayerEntity.class, uuid));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PlayerEntity> findByName(String name) {
        try {
            return Optional.ofNullable(em.createQuery("SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                    .setParameter("name", name)
                    .getSingleResult());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void save(PlayerDto playerDto) {
        PlayerEntity entity = playerMapper.dtoToEntity(playerDto);
        em.persist(entity);
    }

    @Override
    public List<PlayerEntity> findAll() {
        return em.createQuery("SELECT p FROM PlayerEntity p", PlayerEntity.class)
                .getResultList();
    }

    @Override
    public void delete(UUID uuid) {
        PlayerEntity entity = em.find(PlayerEntity.class, uuid);
        if (entity != null) {
            em.remove(entity);
        }
    }

    @Override
    public void update(PlayerDto playerDto) {
        PlayerEntity entity = em.find(PlayerEntity.class, playerDto.getUuid());
        if (entity != null) {
            playerMapper.updateEntityFromDto(playerDto, entity);
            em.merge(entity);
        }
    }
}