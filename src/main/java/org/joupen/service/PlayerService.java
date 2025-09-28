package org.joupen.service;

import lombok.RequiredArgsConstructor;
import org.joupen.domain.PlayerEntity;
import org.joupen.enums.UUIDTypes;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.EventUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository repo;

    public void prolongAll(Duration duration, boolean gift) {
        List<PlayerEntity> players = repo.findAll();
        for (PlayerEntity entity : players) {
            if (!Boolean.TRUE.equals(entity.getPaid()) && !gift) continue;
            prolongOne(entity.getName(), duration, gift);
        }
    }

    public PlayerEntity prolongOne(String name, Duration duration, boolean gift) {
        LocalDateTime now = LocalDateTime.now();

        Optional<PlayerEntity> playerEntityOptional = repo.findByName(name);
        PlayerEntity entity = playerEntityOptional.orElseGet(() -> new PlayerEntity(name, !gift, UUIDTypes.INITIAL_UUID.getUuid(), now.minusDays(1), now.minusDays(1)));

        LocalDateTime base = entity.getValidUntil().isBefore(now) ? now : entity.getValidUntil();
        entity.setValidUntil(base.plus(duration));
        entity.setLastProlongDate(now);

        if (playerEntityOptional.isEmpty()) {
            repo.save(entity);
        } else {
            repo.updateByName(entity, entity.getName());
        }

        EventUtils.publish(new PlayerProlongedEvent(entity, gift, duration));

        return entity;
    }

    public PlayerEntity add(PlayerEntity entity) {
        repo.save(entity);
        return entity;
    }

    public PlayerEntity addAll(List<PlayerEntity> playerEntityList) {
        playerEntityList.forEach(this::add);
        return null;
    }
}
