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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

        checkGiftFile(player);

        // Ищем игрока в репозитории
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
                playerRepository.updateByName(playerDto, playerLoginEvent.getPlayer().getName());

                log.warn("Player {} joined for the first time, adjusted prohodka and changed UUID to {}", playerDto.getName(), playerLoginEvent.getPlayer().getUniqueId());
            } catch (Exception e) {
                log.error("Failed to update player {} in repository: {}", playerDto.getName(), e.getMessage());
                throw new RuntimeException("Failed to update player data", e);
            }
        }

        playerLoginEvent.allow();
    }

    /**
     * Проверка gifts.txt. Если игрок найден — создаётся/обновляется запись в базе и начисляется проходка.
     */
    private void checkGiftFile(Player player) {
        Path giftsFile = Paths.get("plugins/joupen/gifts.txt");
        if (!Files.exists(giftsFile)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(giftsFile, StandardCharsets.UTF_8);
            List<String> updatedLines = new ArrayList<>();
            boolean rewarded = false;

            for (String line : lines) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 2) {
                    updatedLines.add(line); // некорректная строка, оставляем
                    continue;
                }

                String nick = parts[0];
                String reward = parts[1];

                if (nick.equalsIgnoreCase(player.getName())) {
                    LocalDateTime newValidUntil = applyReward(LocalDateTime.now(), reward);

                    // Проверяем — есть ли игрок в базе
                    Optional<PlayerEntity> optionalEntity = playerRepository.findByName(nick);

                    if (optionalEntity.isPresent()) {
                        PlayerDto dto = playerMapper.entityToDto(optionalEntity.get());
                        dto.setValidUntil(newValidUntil);
                        dto.setLastProlongDate(LocalDateTime.now());
                        dto.setUuid(player.getUniqueId());
                        playerRepository.updateByName(dto, nick);
                    } else {
                        PlayerDto dto = new PlayerDto();
                        dto.setName(nick);
                        dto.setUuid(player.getUniqueId());
                        dto.setValidUntil(newValidUntil);
                        dto.setLastProlongDate(LocalDateTime.now());
                        playerRepository.save(dto);
                    }

                    player.sendMessage(Component.text("Ура! Тебе добавили проходку: " + reward, NamedTextColor.GOLD));
                    log.info("Игрок {} получил награду {}", nick, reward);

                    rewarded = true;
                } else {
                    updatedLines.add(line);
                }
            }

            if (rewarded) {
                Files.write(giftsFile, updatedLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Ошибка чтения gifts.txt: {}", e.getMessage());
        }
    }

    private LocalDateTime applyReward(LocalDateTime base, String reward) {
        try {
            if (reward.endsWith("d")) {
                int days = Integer.parseInt(reward.substring(0, reward.length() - 1));
                return base.plusDays(days);
            } else if (reward.endsWith("mo")) {
                int months = Integer.parseInt(reward.substring(0, reward.length() - 2));
                return base.plusMonths(months);
            } else if (reward.endsWith("s")) {
                int seconds = Integer.parseInt(reward.substring(0, reward.length() - 1));
                return base.plusSeconds(seconds);
            } else {
                log.warn("Неизвестный формат награды: {}", reward);
                return base;
            }
        } catch (Exception e) {
            log.error("Ошибка при разборе награды {}: {}", reward, e.getMessage());
            return base;
        }
    }
}
