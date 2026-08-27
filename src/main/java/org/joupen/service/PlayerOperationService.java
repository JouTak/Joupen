package org.joupen.service;

import org.joupen.domain.OperationException;
import org.joupen.domain.OperationMetadata;
import org.joupen.domain.OperationResult;
import org.joupen.domain.OperationType;
import org.joupen.domain.PlayerEntity;
import org.joupen.domain.PlayerOperation;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.Utils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.joupen.enums.UUIDTypes.INITIAL_UUID;

public class PlayerOperationService {
    private final PlayerRepository repo;
    private final Clock clock;

    public PlayerOperationService(PlayerRepository repo) {
        this(repo, Clock.systemDefaultZone());
    }

    public PlayerOperationService(PlayerRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    public OperationResult grant(String name, Duration duration, OperationType type, OperationMetadata metadata) {
        return grant(name, duration, type, metadata, null);
    }

    public OperationResult grant(String name, Duration duration, OperationType type, OperationMetadata metadata, UUID uuid) {
        long seconds = durationSeconds(duration);
        if (seconds < 0 || type != OperationType.PURCHASE && type != OperationType.GIFT && type != OperationType.COMPENSATION) {
            throw new OperationException("invalid-duration");
        }
        return apply(name, metadata, type + ":" + seconds + ":" + uuid, (current, history) -> {
            PlayerEntity after = current.map(PlayerOperation::snapshot).orElseGet(() -> newPlayer(name));
            if (current.isEmpty()) after.setPaid(type == OperationType.PURCHASE);
            if (uuid != null) after.setUuid(uuid);
            LocalDateTime base = after.getValidUntil();
            if (base == null || base.isBefore(now())) base = now();
            after.setValidUntil(base.plusSeconds(seconds));
            if (type == OperationType.PURCHASE) after.setLastProlongDate(now());
            return operation(current, after, type, seconds);
        });
    }

    public OperationResult adjust(String name, Duration duration, OperationMetadata metadata) {
        long seconds = durationSeconds(duration);
        return apply(name, metadata, "ADJUST:" + seconds, (current, history) -> {
            PlayerEntity after = current.map(PlayerOperation::snapshot).orElseGet(() -> newPlayer(name));
            LocalDateTime base = after.getValidUntil();
            if (seconds < 0 && base == null) throw new OperationException("pass-not-found");
            if (seconds > 0 && (base == null || base.isBefore(now()))) base = now();
            after.setValidUntil(base.plusSeconds(seconds));
            return operation(current, after, OperationType.MANUAL_ADJUSTMENT, seconds);
        });
    }

    public OperationResult approval(String name, boolean approved, OperationMetadata metadata) {
        return apply(name, metadata, "APPROVAL:" + approved, (current, history) -> {
            PlayerEntity after = current.map(PlayerOperation::snapshot).orElseGet(() -> newPlayer(name));
            after.setApproved(approved);
            return operation(current, after, OperationType.APPROVAL, 0);
        });
    }

    public OperationResult temporaryAccess(String name, LocalDateTime from, LocalDateTime until, OperationMetadata metadata) {
        if (from == null ^ until == null || from != null && !from.isBefore(until)) {
            throw new OperationException("invalid-window");
        }
        return apply(name, metadata, "TEMPORARY_ACCESS:" + from + ":" + until, (current, history) -> {
            PlayerEntity after = current.map(PlayerOperation::snapshot).orElseGet(() -> newPlayer(name));
            after.setTemporaryAccessFrom(from);
            after.setTemporaryAccessUntil(until);
            return operation(current, after, OperationType.TEMPORARY_ACCESS, 0);
        });
    }

    public OperationResult undo(String name, Long operationId, OperationMetadata metadata) {
        return apply(name, metadata, "UNDO:" + operationId, (current, history) -> {
            Set<Long> reversed = history.stream().map(PlayerOperation::getReversedOperationId)
                    .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            PlayerOperation original = history.stream()
                    .filter(op -> operationId != null ? operationId.equals(op.getId()) : reversible(op) && !reversed.contains(op.getId()))
                    .max(Comparator.comparing(PlayerOperation::getId))
                    .orElseThrow(() -> new OperationException("operation-not-found"));
            if (!reversible(original)) throw new OperationException("operation-not-reversible");
            if (reversed.contains(original.getId())) throw new OperationException("operation-already-reversed");
            PlayerEntity after = current.map(PlayerOperation::snapshot).orElseThrow(() -> new OperationException("player-not-found"));
            long seconds = Math.negateExact(original.getDurationSeconds());
            switch (original.getType()) {
                case APPROVAL -> after.setApproved(original.getBefore() != null && Boolean.TRUE.equals(original.getBefore().getApproved()));
                case TEMPORARY_ACCESS -> {
                    after.setTemporaryAccessFrom(original.getBefore() == null ? null : original.getBefore().getTemporaryAccessFrom());
                    after.setTemporaryAccessUntil(original.getBefore() == null ? null : original.getBefore().getTemporaryAccessUntil());
                }
                default -> {
                    if (after.getValidUntil() == null) throw new OperationException("pass-not-found");
                    after.setValidUntil(after.getValidUntil().plusSeconds(seconds));
                }
            }
            PlayerOperation undo = operation(current, after, OperationType.UNDO, seconds);
            undo.setReversedOperationId(original.getId());
            return undo;
        });
    }

    public OperationResult bindOnLogin(String name, UUID uuid, boolean hasPlayedBefore) {
        OperationMetadata metadata = new OperationMetadata(null, "login", name, uuid.toString());
        return apply(name, metadata, "FIRST_JOIN:" + uuid, (current, history) -> {
            PlayerEntity after = current.map(PlayerOperation::snapshot).orElseThrow(() -> new OperationException("player-not-found"));
            long seconds = 0;
            if (INITIAL_UUID.getUuid().equals(after.getUuid()) && !hasPlayedBefore
                    && after.getValidUntil() != null && after.getLastProlongDate() != null) {
                seconds = Duration.ofDays(ChronoUnit.DAYS.between(after.getLastProlongDate(), now())).getSeconds();
                after.setValidUntil(after.getValidUntil().plusSeconds(seconds));
                after.setLastProlongDate(now());
            }
            after.setUuid(uuid);
            return operation(current, after, OperationType.FIRST_JOIN, seconds);
        });
    }

    public OperationResult importPlayer(PlayerEntity player, OperationMetadata metadata) {
        return apply(player.getName(), metadata, "IMPORT:" + Utils.toJson(player), (current, history) -> {
            if (current.isPresent()) throw new OperationException("player-already-exists");
            LocalDateTime from = player.getLastProlongDate() == null ? now() : player.getLastProlongDate();
            long seconds = player.getValidUntil() == null ? 0 : Duration.between(from, player.getValidUntil()).getSeconds();
            return operation(current, PlayerOperation.snapshot(player), OperationType.IMPORT, seconds);
        });
    }

    public List<PlayerOperation> history(String name, int page) {
        if (page < 1 || page > Integer.MAX_VALUE / 10) throw new OperationException("invalid-page");
        return repo.findHistory(name, 10, (page - 1) * 10);
    }

    public OperationResult migratePlayer(PlayerEntity player, OperationMetadata metadata) {
        validateName(player.getName());
        String target = player.getUuid() == null || INITIAL_UUID.getUuid().equals(player.getUuid()) ? player.getName()
                : repo.findByUuid(player.getUuid()).map(PlayerEntity::getName).orElse(player.getName());
        String request = player.getName().toLowerCase(Locale.ROOT) + ":MIGRATION:" + Utils.toJson(player);
        return repo.applyOperation(target, metadata, request, (current, history) -> {
            LocalDateTime before = current.map(PlayerEntity::getValidUntil).orElse(now());
            long seconds = player.getValidUntil() == null ? 0 : Duration.between(before, player.getValidUntil()).getSeconds();
            return operation(current, PlayerOperation.snapshot(player), OperationType.MIGRATION, seconds);
        });
    }

    private boolean reversible(PlayerOperation operation) {
        return operation.getType() != OperationType.UNDO && operation.getType() != OperationType.FIRST_JOIN
                && operation.getType() != OperationType.MIGRATION;
    }

    private OperationResult apply(String name, OperationMetadata metadata, String request,
                                  BiFunction<Optional<PlayerEntity>, List<PlayerOperation>, PlayerOperation> action) {
        validateName(name);
        return repo.applyOperation(name, metadata, name.toLowerCase(Locale.ROOT) + ":" + request, action);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 16) throw new OperationException("invalid-player");
    }

    private PlayerOperation operation(Optional<PlayerEntity> before, PlayerEntity after, OperationType type, long seconds) {
        return PlayerOperation.builder().type(type).durationSeconds(seconds).occurredAt(now())
                .before(before.map(PlayerOperation::snapshot).orElse(null)).after(after).build();
    }

    private PlayerEntity newPlayer(String name) {
        return new PlayerEntity(name, false, INITIAL_UUID.getUuid(), null, now().minusDays(1));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    private long durationSeconds(Duration duration) {
        if (duration == null || duration.isZero() || duration.getNano() != 0) throw new OperationException("invalid-duration");
        return duration.getSeconds();
    }
}
