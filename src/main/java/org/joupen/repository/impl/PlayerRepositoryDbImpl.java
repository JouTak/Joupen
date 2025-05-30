package org.joupen.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import lombok.extern.slf4j.Slf4j;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.DBRepositoryUtils;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class PlayerRepositoryDbImpl implements PlayerRepository {

    private final EntityManagerFactory emf;
    private final PlayerMapper playerMapper;

    public PlayerRepositoryDbImpl(EntityManager entityManager) {
        if (entityManager == null) {
            throw new IllegalArgumentException("EntityManager cannot be null");
        }
        this.emf = entityManager.getEntityManagerFactory();
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
    }

    @Override
    public EntityManager getEntityManager() {
        if (emf == null || !emf.isOpen()) {
            log.error("EntityManagerFactory is closed");
            throw new IllegalStateException("EntityManagerFactory is closed");
        }
        return emf.createEntityManager();
    }

    @Override
    public Optional<PlayerEntity> findByUuid(UUID uuid) {
        try (EntityManager em = getEntityManager()) {
            return Optional.ofNullable(
                    em.createQuery("SELECT p FROM PlayerEntity p WHERE p.uuid = :uuid", PlayerEntity.class)
                            .setParameter("uuid", uuid)
                            .getSingleResult()
            );
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to find player by UUID {}: {}", uuid, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<PlayerEntity> findByName(String name) {
        try (EntityManager em = getEntityManager()) {
            return Optional.ofNullable(
                    em.createQuery("SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                            .setParameter("name", name)
                            .getSingleResult()
            );
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to find player by name {}: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(PlayerDto playerDto) {
        DBRepositoryUtils.runInTransactionVoid(getEntityManager(), em -> {
            PlayerEntity entity = playerMapper.dtoToEntity(playerDto);
            em.persist(entity);
            log.info("Saved player: {}", playerDto.getName());
        });
    }

    @Override
    public List<PlayerEntity> findAll() {
        try (EntityManager em = getEntityManager()) {
            return em.createQuery("SELECT p FROM PlayerEntity p", PlayerEntity.class)
                    .getResultList();
        }
    }

    @Override
    public void delete(UUID uuid) {
        DBRepositoryUtils.runInTransactionVoid(getEntityManager(), em -> {
            try {
                PlayerEntity entity = em.createQuery("SELECT p FROM PlayerEntity p WHERE p.uuid = :uuid", PlayerEntity.class)
                        .setParameter("uuid", uuid)
                        .getSingleResult();
                em.remove(entity);
                log.info("Deleted player with UUID: {}", uuid);
            } catch (NoResultException e) {
                log.warn("No player found to delete with UUID: {}", uuid);
            }
        });
    }

    @Override
    public void update(PlayerDto playerDto) {
        DBRepositoryUtils.runInTransactionVoid(getEntityManager(), em -> {
            try {
                PlayerEntity entity = em.createQuery("SELECT p FROM PlayerEntity p WHERE p.uuid = :uuid", PlayerEntity.class)
                        .setParameter("uuid", playerDto.getUuid())
                        .getSingleResult();
                playerMapper.updateEntityFromDto(playerDto, entity);
                em.merge(entity);
                log.info("Updated player: {}", playerDto.getName());
            } catch (NoResultException e) {
                log.warn("Player not found for update (NoResultException): {}", playerDto.getName());
            }
        });
    }
}
