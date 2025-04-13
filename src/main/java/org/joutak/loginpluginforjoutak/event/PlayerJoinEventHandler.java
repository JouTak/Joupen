package org.joutak.loginpluginforjoutak.event;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.mapper.PlayerMapper;
import org.joutak.loginpluginforjoutak.repository.PlayerRepository;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.joutak.loginpluginforjoutak.enums.UUIDTypes.INITIAL_UUID;

@Slf4j
public class PlayerJoinEventHandler implements Listener {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    public PlayerJoinEventHandler(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
    }

    @EventHandler
    public void playerJoinEvent(PlayerLoginEvent playerLoginEvent) {
        Player player = playerLoginEvent.getPlayer();

        // Поиск игрока в репозитории
        Optional<PlayerEntity> optionalEntity = playerRepository.findByUuid(player.getUniqueId());
        if (optionalEntity.isEmpty()) {
            optionalEntity = playerRepository.findByName(player.getName());
        }
        PlayerEntity playerEntity = optionalEntity.orElse(null);

        if (playerEntity == null) {
            TextComponent textComponent = Component.text()
                    .append(Component.text("Тебя нет в вайтлисте. Напиши по этому поводу ", NamedTextColor.BLUE))
                    .append(Component.text("EnderDiss'e", NamedTextColor.RED))
                    .build();
            playerLoginEvent.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, textComponent);
            return;
        }

        PlayerDto playerDto = playerMapper.entityToDto(playerEntity);

        // Проверка срока действия подписки
        if (playerDto.getValidUntil().isBefore(LocalDate.now())) {
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
            LocalDate now = LocalDate.now();
            LocalDate validUntil = playerDto.getValidUntil()
                    .plusDays(ChronoUnit.DAYS.between(playerDto.getLastProlongDate(), now));
            playerDto.setValidUntil(validUntil);
            playerDto.setLastProlongDate(now);
            playerDto.setUuid(playerLoginEvent.getPlayer().getUniqueId());

            try {
                playerRepository.update(playerDto);
                log.warn("Player {} joined for the first time, adjusted prohodka and changed UUID", playerDto.getName());
            } catch (Exception e) {
                log.error("Failed to update player {} in repository: {}", playerDto.getName(), e.getMessage());
                playerLoginEvent.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text("Ошибка сервера. Обратитесь к администратору."));
                return;
            }
        }

        playerLoginEvent.allow();
    }
}