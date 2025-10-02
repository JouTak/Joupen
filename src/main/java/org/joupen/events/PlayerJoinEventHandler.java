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

        // Проверяем подарки, если есть ошибка в формате — сразу кикаем
        if (!checkGiftFile(player, playerLoginEvent)) {
            return; // игрок уже кикнут
        }

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

        // Проверка срока действия подписки
        if (playerEntity.getValidUntil().isBefore(LocalDateTime.now())) {
            log.info("У игрока {} была подписка до {}", playerEntity.getName(), playerEntity.getValidUntil());
            TextComponent textComponent = Component.text()
                    .append(Component.text("Проходка кончилась((( Надо оплатить и написать ", NamedTextColor.BLUE))
                    .append(Component.text("EnderDiss'e", NamedTextColor.RED))
                    .build();
            playerLoginEvent.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, textComponent);
            return;
        }

        UUID uuid = playerEntity.getUuid();
        if (uuid.equals(INITIAL_UUID.getUuid())) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime validUntil = playerEntity.getValidUntil()
                    .plusDays(ChronoUnit.DAYS.between(playerEntity.getLastProlongDate(), now));

            playerEntity.setValidUntil(validUntil);
            playerEntity.setLastProlongDate(now);
            playerEntity.setUuid(playerLoginEvent.getPlayer().getUniqueId());

            try {
                playerRepository.updateByName(playerEntity, playerLoginEvent.getPlayer().getName());

                log.warn("Player {} joined for the first time, adjusted prohodka and changed UUID to {}",
                        playerEntity.getName(), playerLoginEvent.getPlayer().getUniqueId());
            } catch (Exception e) {
                log.error("Failed to update playerEntity {} in repository: {}", playerEntity.getName(), e.getMessage());
                throw new RuntimeException("Failed to update playerEntity data", e);
            }
        }

        playerLoginEvent.allow();
    }

    /**
     * Проверка gifts.txt. Если игрок найден — создаётся/обновляется запись в базе и начисляется проходка.
     * Если формат подарка некорректный — игрок кикается.
     *
     * @return true если всё ок, false если игрок кикнут
     */
    private boolean checkGiftFile(Player player, PlayerLoginEvent event) {
        Path giftsFile = Paths.get("plugins/joupen/gifts.txt");
        if (!Files.exists(giftsFile)) {
            return true;
        }

        List<String> updatedLines = new ArrayList<>();
        boolean rewarded = false;

        try (BufferedReader reader = Files.newBufferedReader(giftsFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 2) {
                    updatedLines.add(line); // некорректная строка, оставляем
                    continue;
                }

                String nick = parts[0];
                String reward = parts[1];

                if (nick.equalsIgnoreCase(player.getName())) {
                    Duration duration;
                    try {
                        duration = TimeUtils.parseDuration(reward);
                    } catch (IllegalArgumentException e) {
                        log.error("Невалидный подарок '{}' для игрока {}", reward, nick);
                        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text("Ошибка: неверный формат подарка (" + reward + "). " + "Напиши по этому поводу EnderDiss'e", NamedTextColor.RED));
                        return false;
                    }

                    LocalDateTime newValidUntil = LocalDateTime.now().plus(duration);

                    Optional<PlayerEntity> optionalEntity = playerRepository.findByName(nick);

                    if (optionalEntity.isPresent()) {
                        PlayerEntity dto = optionalEntity.get();
                        dto.setValidUntil(newValidUntil);
                        dto.setLastProlongDate(LocalDateTime.now());
                        dto.setUuid(player.getUniqueId());
                        playerRepository.updateByName(dto, nick);
                    } else {
                        PlayerEntity dto = new PlayerEntity();
                        dto.setName(nick);
                        dto.setUuid(player.getUniqueId());
                        dto.setValidUntil(newValidUntil);
                        dto.setLastProlongDate(LocalDateTime.now());
                        playerRepository.save(dto);
                    }

                    player.sendMessage(Component.text("Ура! Тебе добавили проходку: "
                            + TimeUtils.formatDuration(duration), NamedTextColor.GOLD));
                    log.info("Player {} got a reward {}", nick, reward);

                    rewarded = true;
                } else {
                    updatedLines.add(line);
                }
            }
        } catch (IOException e) {
            log.error("Reading error gifts.txt: {}", e.getMessage());
        }

        if (rewarded) {
            try {
                Files.write(giftsFile, updatedLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                log.error("Writing error gifts.txt: {}", e.getMessage());
            }
        }

        return true;
    }
}
