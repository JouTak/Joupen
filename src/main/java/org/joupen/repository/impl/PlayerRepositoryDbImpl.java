package org.joupen.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.joupen.database.TransactionManager;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;

import org.joupen.jooq.generated.default_schema.tables.Players;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class PlayerRepositoryDbImpl implements PlayerRepository {
    private final PlayerMapper playerMapper;
    private final TransactionManager transactionManager;

    public PlayerRepositoryDbImpl(TransactionManager transactionManager) {
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
        this.transactionManager = transactionManager;
    }

    @Override
    public Optional<PlayerEntity> findByUuid(UUID uuid) {
        return transactionManager.executeInTransactionWithResult(txDsl ->
                txDsl.selectFrom(Players.PLAYERS)
                        .where(Players.PLAYERS.UUID.eq(uuid.toString()))
                        .fetchOptionalInto(PlayerEntity.class)
        );
    }

    @Override
    public Optional<PlayerEntity> findByName(String name) {
        return transactionManager.executeInTransactionWithResult(txDsl ->
                txDsl.selectFrom(Players.PLAYERS)
                        .where(Players.PLAYERS.NAME.eq(name))
                        .fetchOptionalInto(PlayerEntity.class)
        );
    }

    @Override
    public void save(PlayerDto playerDto) {
        transactionManager.executeInTransaction(txDsl -> {
            PlayerEntity entity = playerMapper.dtoToEntity(playerDto);
            txDsl.insertInto(Players.PLAYERS)
                    .set(Players.PLAYERS.UUID, entity.getUuid().toString())
                    .set(Players.PLAYERS.NAME, entity.getName())
                    .set(Players.PLAYERS.VALID_UNTIL, entity.getValidUntil())
                    .set(Players.PLAYERS.LAST_PROLONG_DATE, entity.getLastProlongDate())
//                    .set(Players.PLAYERS.PAID, entity.getPaid() ? (byte) 1 : (byte) 0)
                    .onDuplicateKeyUpdate()
                    .set(Players.PLAYERS.NAME, entity.getName())
                    .set(Players.PLAYERS.VALID_UNTIL, entity.getValidUntil())
                    .set(Players.PLAYERS.LAST_PROLONG_DATE, entity.getLastProlongDate())
//                    .set(Players.PLAYERS.PAID, entity.getPaid() ? (byte) 1 : (byte) 0)
                    .execute();
        });
    }

    @Override
    public List<PlayerEntity> findAll() {
        return transactionManager.executeInTransactionWithResult(txDsl ->
                txDsl.selectFrom(Players.PLAYERS)
                        .fetchInto(PlayerEntity.class)
        );
    }

    @Override
    public void updateByName(PlayerDto playerDto, String name) {
        transactionManager.executeInTransaction(txDsl -> {
            PlayerEntity entity = playerMapper.dtoToEntity(playerDto);
            int rowsAffected = txDsl.update(Players.PLAYERS)
                    .set(Players.PLAYERS.UUID, entity.getUuid().toString())
                    .set(Players.PLAYERS.NAME, entity.getName())
                    .set(Players.PLAYERS.VALID_UNTIL, entity.getValidUntil())
                    .set(Players.PLAYERS.LAST_PROLONG_DATE, entity.getLastProlongDate())
//                    .set(Players.PLAYERS.PAID, entity.getPaid() ? (byte) 1 : (byte) 0)
                    .where(Players.PLAYERS.NAME.eq(name))
                    .execute();
            if (rowsAffected == 0) {
                log.warn("No rows updated for player {} with new UUID {}", name, entity.getUuid());
            } else {
                log.info("Updated {} rows for player {} with new UUID {}", rowsAffected, name, entity.getUuid());
            }
        });
    }
    @Override
    public void updateByUuid(PlayerDto playerDto,UUID uuid) {
        transactionManager.executeInTransaction(txDsl -> {
            PlayerEntity entity = playerMapper.dtoToEntity(playerDto);
            int rowsAffected = txDsl.update(Players.PLAYERS)
                    .set(Players.PLAYERS.UUID, entity.getUuid().toString())
                    .set(Players.PLAYERS.NAME, entity.getName())
                    .set(Players.PLAYERS.VALID_UNTIL, entity.getValidUntil())
                    .set(Players.PLAYERS.LAST_PROLONG_DATE, entity.getLastProlongDate())
//                    .set(Players.PLAYERS.PAID, entity.getPaid() ? (byte) 1 : (byte) 0)
                    .where(Players.PLAYERS.UUID.eq(uuid.toString()))
                    .execute();
            if (rowsAffected == 0) {
                log.warn("No rows updated for player {} with UUID {}", entity.getName(), entity.getUuid());
            } else {
                log.info("Updated {} rows for player {} with UUID {}", rowsAffected, entity.getName(), entity.getUuid());
            }
        });
    }
    @Override
    public void delete(UUID uuid) {
        transactionManager.executeInTransaction(txDsl ->
                txDsl.deleteFrom(Players.PLAYERS)
                        .where(Players.PLAYERS.UUID.eq(uuid.toString()))
                        .execute()
        );
    }
}