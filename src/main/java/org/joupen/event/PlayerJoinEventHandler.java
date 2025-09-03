package org.joupen.event;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.joupen.database.TransactionManager;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.joupen.enums.UUIDTypes.INITIAL_UUID;

@Slf4j
public class PlayerJoinEventHandler implements Listener {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final TransactionManager transactionManager;

    public PlayerJoinEventHandler(PlayerRepository playerRepository, TransactionManager transactionManager) {
        this.playerRepository = playerRepository;
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
        this.transactionManager = transactionManager;
    }

    @EventHandler
    public void playerJoinEvent(PlayerLoginEvent playerLoginEvent) {
        Player player = playerLoginEvent.getPlayer();

        // Поиск игрока в репозитории
        Optional<PlayerEntity> optionalEntity = playerRepository.findByUuid(player.getUniqueId());
        if (optionalEntity.isEmpty()) {
            optionalEntity = playerRepository.findByName(player.getName());
        }

        if (optionalEntity.isEmpty()) {
            TextComponent textComponent = Component.text()
                    .append(Component.text("Тебя нет в вайтлисте. Напиши по этому поводу ", NamedTextColor.BLUE))
                    .append(Component.text("EnderDiss'e", NamedTextColor.RED))
                    .build();
            playerLoginEvent.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, textComponent);
            return;
        }

        PlayerDto playerDto = playerMapper.entityToDto(optionalEntity.get());

        // Проверка срока действия подписки
        if (playerDto.getValidUntil().isBefore(LocalDateTime.now())) {
            log.info("У игрока {} была подписка до {}", player.getName(), playerDto.getValidUntil());
            TextComponent textComponent = Component.text()
                    .append(Component.text("Проходка кончилась((( Надо оплатить и написать ", NamedTextColor.BLUE))
                    .append(Component.text("EnderDiss'e", NamedTextColor.RED))
                    .build();
            playerLoginEvent.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, textComponent);
            return;
        }

        // Обновление UUID и продление подписки
        UUID uuid = playerDto.getUuid();
        if (uuid.equals(INITIAL_UUID.getUuid())) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime validUntil = playerDto.getValidUntil().plusDays(ChronoUnit.DAYS.between(playerDto.getLastProlongDate(), now));

            playerDto.setValidUntil(validUntil);
            playerDto.setLastProlongDate(now);
            playerDto.setUuid(playerLoginEvent.getPlayer().getUniqueId());

            try {
                playerRepository.update(playerDto);
                log.warn("Player {} joined for the first time, adjusted prohodka and changed UUID", playerDto.getName());
            } catch (Exception e) {
                log.error("Failed to update player {} in repository: {}", playerDto.getName(), e.getMessage());
                throw new RuntimeException("Failed to update player data", e);
            }
        }

        playerLoginEvent.allow();
    }
}