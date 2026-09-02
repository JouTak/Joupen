package org.joupen.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.joupen.bukkit.event.JoupenPassProlongedEvent;
import org.joupen.domain.PlayerEntity;
import org.joupen.domain.OperationMetadata;
import org.joupen.domain.OperationResult;
import org.joupen.domain.OperationType;
import org.joupen.domain.PlayerOperation;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.EventUtils;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Slf4j
public class PlayerService {
    private final PlayerRepository repo;
    @Getter
    private final PlayerOperationService operations;

    public PlayerService(PlayerRepository repo) {
        this.repo = repo;
        this.operations = new PlayerOperationService(repo);
    }

    public void prolongAll(Duration duration, boolean gift) {
        prolongAll(duration, gift, OperationMetadata.system("api"));
    }

    public void prolongAll(Duration duration, boolean gift, OperationMetadata metadata) {
        List<PlayerEntity> players = repo.findAll();
        for (PlayerEntity entity : players) {
            if (!Boolean.TRUE.equals(entity.getPaid()) && !gift) continue;
            OperationMetadata perPlayer = metadata.externalId() == null ? metadata : new OperationMetadata(
                    metadata.reason(), metadata.source(), metadata.initiator(),
                    metadata.externalId() + ":" + entity.getName().toLowerCase(Locale.ROOT));
            prolongOne(entity.getName(), duration, gift, perPlayer);
        }
    }

    public PlayerEntity prolongOne(String name, Duration duration, boolean gift) {
        return prolongOne(name, duration, gift, OperationMetadata.system("api")).operation().getAfter();
    }

    public OperationResult prolongOne(String name, Duration duration, boolean gift, OperationMetadata metadata) {
        return grant(name, duration, gift ? OperationType.GIFT : OperationType.PURCHASE, metadata);
    }

    public OperationResult grant(String name, Duration duration, OperationType type, OperationMetadata metadata) {
        OperationResult result = operations.grant(name, duration, type, metadata);
        if (!result.applied()) return result;
        PlayerEntity entity = PlayerOperation.snapshot(result.operation().getAfter());
        boolean gift = type != OperationType.PURCHASE;
        try {
            EventUtils.publish(new PlayerProlongedEvent(entity, gift, duration));
        } catch (RuntimeException e) {
            log.error("Failed to publish operation {}", result.operation().getId(), e);
        }
        try {
            Bukkit.getPluginManager().callEvent(new JoupenPassProlongedEvent(entity.getUuid(), entity.getName(), gift, duration, entity.getValidUntil()));
        } catch (RuntimeException e) {
            log.error("Failed to publish Bukkit event for operation {}", result.operation().getId(), e);
        }
        return result;
    }

    public PlayerEntity add(PlayerEntity entity) {
        return operations.importPlayer(entity, OperationMetadata.system("import")).operation().getAfter();
    }

    public void addAll(List<PlayerEntity> playerEntityList) {
        playerEntityList.forEach(this::add);
    }

    public void addAll(List<PlayerEntity> players, OperationMetadata metadata) {
        players.forEach(player -> operations.importPlayer(player, metadata));
    }
}
