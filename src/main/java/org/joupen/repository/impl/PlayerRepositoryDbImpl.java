package org.joupen.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.joupen.database.TransactionManager;
import org.joupen.domain.PlayerEntity;
import org.joupen.domain.OperationMetadata;
import org.joupen.domain.OperationResult;
import org.joupen.domain.OperationType;
import org.joupen.domain.PlayerOperation;
import org.joupen.jooq.generated.tables.Players;
import org.joupen.jooq.generated.tables.records.PlayerOperationsRecord;
import org.joupen.jooq.generated.tables.records.PlayersRecord;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.Utils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.joupen.jooq.generated.tables.OperationLock.OPERATION_LOCK;
import static org.joupen.jooq.generated.tables.PlayerOperations.PLAYER_OPERATIONS;

@Slf4j
public class PlayerRepositoryDbImpl implements PlayerRepository {
    private final TransactionManager transactionManager;

    public PlayerRepositoryDbImpl(TransactionManager transactionManager) {
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
    public void save(PlayerEntity entity) {
        transactionManager.executeInTransaction(txDsl -> {
            txDsl.insertInto(Players.PLAYERS)
                    .set(Players.PLAYERS.UUID, entity.getUuid().toString())
                    .set(Players.PLAYERS.NAME, entity.getName())
                    .set(Players.PLAYERS.VALID_UNTIL, entity.getValidUntil())
                    .set(Players.PLAYERS.LAST_PROLONG_DATE, entity.getLastProlongDate())
                    .set(Players.PLAYERS.PAID, entity.getPaid())
                    .set(Players.PLAYERS.APPROVED, Boolean.TRUE.equals(entity.getApproved()))
                    .set(Players.PLAYERS.TEMPORARY_ACCESS_FROM, entity.getTemporaryAccessFrom())
                    .set(Players.PLAYERS.TEMPORARY_ACCESS_UNTIL, entity.getTemporaryAccessUntil())
                    .onDuplicateKeyUpdate()
                    .set(Players.PLAYERS.NAME, entity.getName())
                    .set(Players.PLAYERS.VALID_UNTIL, entity.getValidUntil())
                    .set(Players.PLAYERS.LAST_PROLONG_DATE, entity.getLastProlongDate())
                    .set(Players.PLAYERS.PAID, entity.getPaid())
                    .set(Players.PLAYERS.APPROVED, Boolean.TRUE.equals(entity.getApproved()))
                    .set(Players.PLAYERS.TEMPORARY_ACCESS_FROM, entity.getTemporaryAccessFrom())
                    .set(Players.PLAYERS.TEMPORARY_ACCESS_UNTIL, entity.getTemporaryAccessUntil())
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
    public void updateByName(PlayerEntity entity, String name) {
        transactionManager.executeInTransaction(txDsl -> {
            int rowsAffected = txDsl.update(Players.PLAYERS)
                    .set(Players.PLAYERS.UUID, entity.getUuid().toString())
                    .set(Players.PLAYERS.NAME, entity.getName())
                    .set(Players.PLAYERS.VALID_UNTIL, entity.getValidUntil())
                    .set(Players.PLAYERS.LAST_PROLONG_DATE, entity.getLastProlongDate())
                    .set(Players.PLAYERS.PAID, entity.getPaid())
                    .set(Players.PLAYERS.APPROVED, Boolean.TRUE.equals(entity.getApproved()))
                    .set(Players.PLAYERS.TEMPORARY_ACCESS_FROM, entity.getTemporaryAccessFrom())
                    .set(Players.PLAYERS.TEMPORARY_ACCESS_UNTIL, entity.getTemporaryAccessUntil())
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
    public void updateByUuid(PlayerEntity entity, UUID uuid) {
        transactionManager.executeInTransaction(txDsl -> {
            int rowsAffected = txDsl.update(Players.PLAYERS)
                    .set(Players.PLAYERS.UUID, entity.getUuid().toString())
                    .set(Players.PLAYERS.NAME, entity.getName())
                    .set(Players.PLAYERS.VALID_UNTIL, entity.getValidUntil())
                    .set(Players.PLAYERS.LAST_PROLONG_DATE, entity.getLastProlongDate())
                    .set(Players.PLAYERS.PAID, entity.getPaid())
                    .set(Players.PLAYERS.APPROVED, Boolean.TRUE.equals(entity.getApproved()))
                    .set(Players.PLAYERS.TEMPORARY_ACCESS_FROM, entity.getTemporaryAccessFrom())
                    .set(Players.PLAYERS.TEMPORARY_ACCESS_UNTIL, entity.getTemporaryAccessUntil())
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

    @Override
    public OperationResult applyOperation(String name, OperationMetadata metadata, String request,
                                          BiFunction<Optional<PlayerEntity>, List<PlayerOperation>, PlayerOperation> action) {
        return transactionManager.executeInTransactionWithResult(txDsl -> {
            txDsl.selectFrom(OPERATION_LOCK).where(OPERATION_LOCK.ID.eq(1)).forUpdate().fetchSingle();
            if (metadata.externalId() != null) {
                Optional<PlayerOperation> duplicate = txDsl.selectFrom(PLAYER_OPERATIONS)
                        .where(PLAYER_OPERATIONS.EXTERNAL_KEY.eq(metadata.externalKey()))
                        .fetchOptional(this::mapOperation);
                if (duplicate.isPresent()) return OperationResult.duplicate(duplicate.get(), request);
            }
            Optional<PlayerEntity> current = txDsl.selectFrom(Players.PLAYERS)
                    .where(Players.PLAYERS.NAME.equalIgnoreCase(name)).fetchOptionalInto(PlayerEntity.class);
            List<PlayerOperation> history = current.map(player -> txDsl.selectFrom(PLAYER_OPERATIONS)
                    .where(PLAYER_OPERATIONS.PLAYER_ID.eq(player.getId()))
                    .orderBy(PLAYER_OPERATIONS.ID).fetch(this::mapOperation)).orElseGet(List::of);
            PlayerOperation operation = action.apply(current.map(PlayerOperation::snapshot), history);
            PlayerEntity after = operation.getAfter();
            after.setApproved(Boolean.TRUE.equals(after.getApproved()));
            PlayersRecord player = txDsl.newRecord(Players.PLAYERS);
            player.setUuid(after.getUuid().toString());
            player.setName(after.getName());
            player.setValidUntil(after.getValidUntil());
            player.setLastProlongDate(after.getLastProlongDate());
            player.setPaid(after.getPaid());
            player.setApproved(after.getApproved());
            player.setTemporaryAccessFrom(after.getTemporaryAccessFrom());
            player.setTemporaryAccessUntil(after.getTemporaryAccessUntil());
            if (current.isPresent()) {
                player.setId(current.get().getId());
                player.update();
            } else {
                player.insert();
            }
            after.setId(player.getId());
            operation.setMetadata(metadata);
            operation.setRequest(request);
            PlayerOperationsRecord record = txDsl.newRecord(PLAYER_OPERATIONS);
            record.setPlayerId(after.getId());
            record.setPlayerName(after.getName());
            record.setType(operation.getType().name());
            record.setDurationSeconds(operation.getDurationSeconds());
            record.setOccurredAt(operation.getOccurredAt());
            record.setReason(metadata.reason());
            record.setSource(metadata.source());
            record.setInitiator(metadata.initiator());
            record.setExternalId(metadata.externalId());
            record.setExternalKey(metadata.externalKey());
            record.setRequest(request);
            record.setReversedOperationId(operation.getReversedOperationId());
            record.setStateBefore(operation.getBefore() == null ? null : Utils.toJson(operation.getBefore()));
            record.setStateAfter(Utils.toJson(after));
            record.insert();
            operation.setId(record.getId());
            return new OperationResult(operation, true);
        });
    }

    @Override
    public List<PlayerOperation> findHistory(String name, int limit, int offset) {
        return transactionManager.executeInTransactionWithResult(txDsl -> txDsl.selectFrom(PLAYER_OPERATIONS)
                .where(PLAYER_OPERATIONS.PLAYER_NAME.equalIgnoreCase(name)
                        .or(PLAYER_OPERATIONS.PLAYER_ID.in(txDsl.select(Players.PLAYERS.ID)
                                .from(Players.PLAYERS).where(Players.PLAYERS.NAME.equalIgnoreCase(name)))))
                .orderBy(PLAYER_OPERATIONS.ID.desc()).limit(limit).offset(offset).fetch(this::mapOperation));
    }

    private PlayerOperation mapOperation(PlayerOperationsRecord record) {
        return PlayerOperation.builder()
                .id(record.getId())
                .type(OperationType.valueOf(record.getType()))
                .durationSeconds(record.getDurationSeconds())
                .occurredAt(record.getOccurredAt())
                .metadata(new OperationMetadata(record.getReason(), record.getSource(), record.getInitiator(), record.getExternalId()))
                .request(record.getRequest())
                .reversedOperationId(record.getReversedOperationId())
                .before(record.getStateBefore() == null ? null : Utils.fromJson(record.getStateBefore(), PlayerEntity.class))
                .after(Utils.fromJson(record.getStateAfter(), PlayerEntity.class))
                .build();
    }
}
