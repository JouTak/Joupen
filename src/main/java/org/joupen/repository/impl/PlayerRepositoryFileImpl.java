package org.joupen.repository.impl;

import com.google.gson.reflect.TypeToken;
import org.joupen.domain.OperationMetadata;
import org.joupen.domain.OperationResult;
import org.joupen.domain.PlayerEntity;
import org.joupen.domain.PlayerOperation;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.JoupenProperties;
import org.joupen.utils.FileUtils;
import org.joupen.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class PlayerRepositoryFileImpl implements PlayerRepository {
    private static final Object FILE_LOCK = new Object();

    @Override
    public Optional<PlayerEntity> findByUuid(UUID uuid) {
        synchronized (FILE_LOCK) {
            return readPlayers().stream().filter(player -> !player.deleted && uuid.equals(player.getUuid()))
                    .findFirst().map(PlayerOperation::snapshot);
        }
    }

    @Override
    public Optional<PlayerEntity> findByName(String name) {
        synchronized (FILE_LOCK) {
            return findPlayer(readPlayers(), name).map(PlayerOperation::snapshot);
        }
    }

    @Override
    public List<PlayerEntity> findAll() {
        synchronized (FILE_LOCK) {
            return readPlayers().stream().filter(player -> !player.deleted).map(PlayerOperation::snapshot).toList();
        }
    }

    @Override
    public void save(PlayerEntity entity) {
        synchronized (FILE_LOCK) {
            List<FilePlayer> players = readPlayers();
            if (entity.getId() == null) entity.setId(nextPlayerId(players));
            players.add(new FilePlayer(entity, List.of()));
            writePlayers(players);
        }
    }

    @Override
    public void updateByUuid(PlayerEntity entity, UUID uuid) {
        update(entity, player -> uuid.equals(player.getUuid()));
    }

    @Override
    public void updateByName(PlayerEntity entity, String name) {
        update(entity, player -> player.getName().equalsIgnoreCase(name));
    }

    private void update(PlayerEntity entity, Predicate<FilePlayer> filter) {
        synchronized (FILE_LOCK) {
            List<FilePlayer> players = readPlayers();
            for (int i = 0; i < players.size(); i++) {
                FilePlayer current = players.get(i);
                if (!current.deleted && filter.test(current)) {
                    entity.setId(current.getId());
                    players.set(i, new FilePlayer(entity, current.operations));
                    writePlayers(players);
                    return;
                }
            }
        }
    }

    @Override
    public void delete(UUID uuid) {
        synchronized (FILE_LOCK) {
            List<FilePlayer> players = readPlayers();
            players.stream().filter(player -> uuid.equals(player.getUuid())).forEach(player -> player.deleted = true);
            writePlayers(players);
        }
    }

    @Override
    public OperationResult applyOperation(String name, OperationMetadata metadata, String request,
                                          BiFunction<Optional<PlayerEntity>, List<PlayerOperation>, PlayerOperation> action) {
        synchronized (FILE_LOCK) {
            List<FilePlayer> players = readPlayers();
            if (metadata.externalId() != null) {
                Optional<PlayerOperation> duplicate = players.stream().flatMap(player -> player.operations.stream())
                        .filter(operation -> metadata.externalKey().equals(operation.getMetadata().externalKey()))
                        .findFirst();
                if (duplicate.isPresent()) return OperationResult.duplicate(duplicate.get(), request);
            }
            Optional<FilePlayer> current = findPlayer(players, name);
            List<PlayerOperation> history = current.map(player -> player.operations).orElseGet(ArrayList::new);
            PlayerOperation operation = action.apply(current.map(PlayerOperation::snapshot), List.copyOf(history));
            PlayerEntity after = operation.getAfter();
            after.setId(current.map(PlayerEntity::getId).orElseGet(() -> nextPlayerId(players)));
            operation.setId(players.stream().flatMap(player -> player.operations.stream())
                    .mapToLong(PlayerOperation::getId).max().orElse(0) + 1);
            operation.setMetadata(metadata);
            operation.setRequest(request);
            List<PlayerOperation> updatedHistory = new ArrayList<>(history);
            updatedHistory.add(operation);
            FilePlayer updated = new FilePlayer(after, updatedHistory);
            if (current.isPresent()) players.set(players.indexOf(current.get()), updated);
            else players.add(updated);
            writePlayers(players);
            return new OperationResult(operation, true);
        }
    }

    @Override
    public List<PlayerOperation> findHistory(String name, int limit, int offset) {
        synchronized (FILE_LOCK) {
            return readPlayers().stream().filter(player -> player.getName().equalsIgnoreCase(name))
                    .flatMap(player -> player.operations.stream())
                    .sorted(Comparator.comparing(PlayerOperation::getId).reversed())
                    .skip(offset).limit(limit).toList();
        }
    }

    private Optional<FilePlayer> findPlayer(List<FilePlayer> players, String name) {
        return players.stream().filter(player -> !player.deleted && player.getName().equalsIgnoreCase(name)).findFirst();
    }

    private long nextPlayerId(List<FilePlayer> players) {
        return players.stream().map(PlayerEntity::getId).filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue).max().orElse(0) + 1;
    }

    private List<FilePlayer> readPlayers() {
        Path path = Path.of(JoupenProperties.playersFilepath);
        if (!Files.exists(path)) return new ArrayList<>();
        try {
            List<FilePlayer> players = Utils.fromJson(Files.readString(path), new TypeToken<List<FilePlayer>>() {}.getType());
            if (players == null) throw new IllegalStateException("Invalid player file: " + path);
            long nextId = nextPlayerId(players);
            for (FilePlayer player : players) {
                if (player.getId() == null) player.setId(nextId++);
                if (player.getApproved() == null) player.setApproved(true);
                if (player.operations == null) player.operations = new ArrayList<>();
            }
            return players;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read players: " + path, e);
        }
    }

    private void writePlayers(List<FilePlayer> players) {
        Path path = Path.of(JoupenProperties.playersFilepath).toAbsolutePath();
        String json = Utils.toJson(players);
        if (json == null) throw new IllegalStateException("Failed to serialize players");
        try {
            FileUtils.writeAtomic(path, json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write players: " + path, e);
        }
    }

    private static class FilePlayer extends PlayerEntity {
        private List<PlayerOperation> operations = new ArrayList<>();
        private boolean deleted;

        private FilePlayer() {
        }

        private FilePlayer(PlayerEntity player, List<PlayerOperation> operations) {
            super(player.getId(), player.getUuid(), player.getName(), player.getValidUntil(), player.getLastProlongDate(),
                    player.getPaid(), Boolean.TRUE.equals(player.getApproved()), player.getTemporaryAccessFrom(),
                    player.getTemporaryAccessUntil());
            this.operations = new ArrayList<>(operations);
        }
    }
}

