package org.joupen.repository;

import org.joupen.domain.PlayerEntity;
import org.joupen.domain.OperationMetadata;
import org.joupen.domain.OperationResult;
import org.joupen.domain.PlayerOperation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public interface PlayerRepository {
    Optional<PlayerEntity> findByUuid(UUID uuid);

    Optional<PlayerEntity> findByName(String name);

    void save(PlayerEntity playerDto);

    List<PlayerEntity> findAll();

    void updateByUuid(PlayerEntity playerDto, UUID uuid);

    void delete(UUID uuid);

    void updateByName(PlayerEntity playerDto, String name);

    OperationResult applyOperation(String name, OperationMetadata metadata, String request,
                                   BiFunction<Optional<PlayerEntity>, List<PlayerOperation>, PlayerOperation> action);

    List<PlayerOperation> findHistory(String name, int limit, int offset);
}
