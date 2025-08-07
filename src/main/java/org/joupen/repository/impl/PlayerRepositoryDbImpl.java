package org.joupen.repository.impl;

import jakarta.persistence.EntityManager;
import org.joupen.database.TransactionManager;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerRepositoryDbImpl implements PlayerRepository {
    private final EntityManager em;
    private final PlayerMapper playerMapper;
    private final TransactionManager transactionManager;

    public PlayerRepositoryDbImpl(EntityManager entityManager, TransactionManager transactionManager) {
        if (entityManager == null) {
            throw new IllegalArgumentException("EntityManager cannot be null");
        }
        this.em = entityManager;
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
        this.transactionManager = transactionManager;
    }

    @Override
    public Optional<PlayerEntity> findByUuid(UUID uuid) {
        return transactionManager.executeInTransactionWithResult(em ->
                em.createQuery("SELECT p FROM PlayerEntity p WHERE p.uuid = :uuid", PlayerEntity.class)
                        .setParameter("uuid", uuid)
                        .getResultStream()
                        .findFirst()
        );
    }

    @Override
    public Optional<PlayerEntity> findByName(String name) {
        return transactionManager.executeInTransactionWithResult(em ->
                em.createQuery("SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                        .setParameter("name", name)
                        .getResultStream()
                        .findFirst()
        );
    }

    @Override
    public void save(PlayerDto playerDto) {
        transactionManager.executeInTransaction(em -> {
            PlayerEntity entity = playerMapper.dtoToEntity(playerDto);
            if (entity.getId() == null) {
                em.persist(entity); // Создаём новую сущность (используется при миграции)
            } else {
                em.merge(entity); // Обновляем существующую сущность
            }
        });
    }

    @Override
    public List<PlayerEntity> findAll() {
        return transactionManager.executeInTransactionWithResult(em ->
                em.createQuery("SELECT p FROM PlayerEntity p", PlayerEntity.class).getResultList()
        );
    }

    @Override
    public void update(PlayerDto playerDto) {
        transactionManager.executeInTransaction(em -> {
            PlayerEntity entity = em.createQuery(
                            "SELECT p FROM PlayerEntity p WHERE p.uuid = :uuid",
                            PlayerEntity.class)
                    .setParameter("uuid", playerDto.getUuid())
                    .getSingleResult();

            if (entity != null) {
                playerMapper.updateEntityFromDto(playerDto, entity);
                em.merge(entity);
            }
        });
    }


    @Override
    public EntityManager getEntityManager() {
        return null;
    }

    @Override
    public void delete(UUID uuid) {
        transactionManager.executeInTransaction(em -> {
            PlayerEntity entity = em.find(PlayerEntity.class, uuid);
            if (entity != null) {
                em.remove(entity);
            }
        });
    }
}