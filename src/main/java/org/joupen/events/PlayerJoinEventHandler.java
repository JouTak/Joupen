package org.joupen.events;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.joupen.domain.PlayerEntity;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.TimeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.joupen.enums.UUIDTypes.INITIAL_UUID;

@Slf4j
public class PlayerJoinEventHandler implements Listener {

    private final PlayerRepository playerRepository;

    public PlayerJoinEventHandler(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @EventHandler
    public void playerJoinEvent(PlayerLoginEvent playerLoginEvent) {
        Player player = playerLoginEvent.getPlayer();

        // Проверяем подарки, но не кикаем при ошибках формата
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

        PlayerEntity playerEntity = optionalEntity.get();

        // Проверка срока действия проходки
        if (playerEntity.getValidUntil().isBefore(LocalDateTime.now())) {
            log.info("У игрока {} была подписка до {}", playerEntity.getName(), playerEntity.getValidUntil());
            TextComponent textComponent = Component.text()
                    .append(Component.text("Проходка кончилась((( Надо оплатить и написать ", NamedTextColor.BLUE))
                    .append(Component.text("EnderDiss'e", NamedTextColor.RED))
                    .build();
            playerLoginEvent.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, textComponent);
            return;
        }

        // Обновляем UUID и продлеваем подписку, если это первый вход
        UUID uuid = playerEntity.getUuid();
        if (uuid.equals(INITIAL_UUID.getUuid())) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime validUntil = playerEntity.getValidUntil()
                    .plusDays(ChronoUnit.DAYS.between(playerEntity.getLastProlongDate(), now));

            playerEntity.setValidUntil(validUntil);
            playerEntity.setLastProlongDate(now);
            playerEntity.setUuid(player.getUniqueId());

            try {
                playerRepository.updateByName(playerEntity, player.getName());
                log.warn("Player {} joined for the first time, adjusted prohodka and changed UUID to {}",
                        playerEntity.getName(), player.getUniqueId());
            } catch (Exception e) {
                log.error("Failed to update player {} in repository: {}", playerEntity.getName(), e.getMessage());
                throw new RuntimeException("Failed to update player data", e);
            }
        }

        playerLoginEvent.allow();
    }

    /**
     * Проверяет файл gifts.txt. Если игрок найден — создаётся или обновляется запись в базе и начисляется проходка.
     * Если формат подарка некорректный — игроку выводится сообщение, но он не кикается.
     */
    private void checkGiftFile(Player player) {
        Path giftsFile = Paths.get("plugins/joupen/gifts.txt");
        if (!Files.exists(giftsFile)) return;

        List<String> updatedLines = new ArrayList<>();
        boolean rewarded = false;

        try (BufferedReader reader = Files.newBufferedReader(giftsFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 2) {
                    updatedLines.add(line);
                    continue;
                }

                String nick = parts[0];
                String reward = parts[1];

                if (nick.equalsIgnoreCase(player.getName())) {
                    Duration duration;
                    try {
                        duration = TimeUtils.parseDuration(reward);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid gift '{}' for player {}", reward, nick);
                        player.sendMessage(Component.text(
                                "Ошибка: неверный формат подарка (" + reward + "). Обратись к EnderDiss'e",
                                NamedTextColor.RED
                        ));
                        updatedLines.add(line); // оставляем строку
                        continue;
                    }

                    LocalDateTime now = LocalDateTime.now();
                    Optional<PlayerEntity> optionalEntity = playerRepository.findByName(nick);

                    PlayerEntity entity;
                    if (optionalEntity.isPresent()) {
                        entity = optionalEntity.get();
                        LocalDateTime base = entity.getValidUntil().isBefore(now) ? now : entity.getValidUntil();
                        entity.setValidUntil(base.plus(duration));
                        entity.setLastProlongDate(now);
                        entity.setUuid(player.getUniqueId());
                        playerRepository.updateByName(entity, nick);
                    } else {
                        entity = new PlayerEntity();
                        entity.setName(nick);
                        entity.setUuid(player.getUniqueId());
                        entity.setValidUntil(now.plus(duration));
                        entity.setLastProlongDate(now);
                        entity.setPaid(false);
                        playerRepository.save(entity);
                    }

                    player.sendMessage(Component.text(
                            "Ура! Тебе добавили проходку: " + TimeUtils.formatDuration(duration),
                            NamedTextColor.GOLD
                    ));
                    log.info("Player {} got a reward {}", nick, reward);

                    rewarded = true;
                } else {
                    updatedLines.add(line);
                }
            }
        } catch (IOException e) {
            log.error("Error reading gifts.txt: {}", e.getMessage());
        }

        if (rewarded) {
            try {
                Files.write(giftsFile, updatedLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                log.error("Error writing gifts.txt: {}", e.getMessage());
            }
        }
    }
}
