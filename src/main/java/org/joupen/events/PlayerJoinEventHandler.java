package org.joupen.events;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.joupen.domain.PlayerEntity;
import org.joupen.domain.OperationMetadata;
import org.joupen.domain.OperationResult;
import org.joupen.domain.OperationType;
import org.joupen.repository.PlayerRepository;
import org.joupen.service.AccessDecision;
import org.joupen.service.PlayerAccessService;
import org.joupen.service.PlayerOperationService;
import org.joupen.service.GiftQueueFile;
import org.joupen.utils.JoupenProperties;
import org.joupen.utils.TimeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.joupen.enums.UUIDTypes.INITIAL_UUID;

@Slf4j
public class PlayerJoinEventHandler implements Listener {

    private final PlayerRepository playerRepository;
    private final PlayerAccessService playerAccessService;
    private final PlayerOperationService operations;
    private final Path giftsFile;

    public PlayerJoinEventHandler(PlayerRepository playerRepository) {
        this(playerRepository, Paths.get("plugins/joupen/gifts.txt"));
    }

    public PlayerJoinEventHandler(PlayerRepository playerRepository, Path giftsFile) {
        this.playerRepository = playerRepository;
        this.playerAccessService = new PlayerAccessService();
        this.operations = new PlayerOperationService(playerRepository);
        this.giftsFile = giftsFile;
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

        boolean hasPlayedBefore = player.hasPlayedBefore();
        LocalDateTime now = LocalDateTime.now();
        AccessDecision decision = playerAccessService.evaluate(optionalEntity, hasPlayedBefore, now);

        if (decision == AccessDecision.APPROVAL_REQUIRED) {
            playerLoginEvent.disallow(
                    PlayerLoginEvent.Result.KICK_WHITELIST,
                    Component.text(JoupenProperties.approvalRequiredMessage, NamedTextColor.BLUE)
            );
            return;
        }

        if (decision == AccessDecision.PASS_REQUIRED) {
            playerLoginEvent.disallow(
                    PlayerLoginEvent.Result.KICK_WHITELIST,
                    Component.text(JoupenProperties.passRequiredMessage, NamedTextColor.BLUE)
            );
            return;
        }

        PlayerEntity playerEntity = optionalEntity.orElseThrow();

        // Обновляем UUID и продлеваем подписку, если это первый вход
        UUID uuid = playerEntity.getUuid();
        if (uuid.equals(INITIAL_UUID.getUuid())) {
            try {
                operations.bindOnLogin(playerEntity.getName(), player.getUniqueId(), hasPlayedBefore);
                log.info("Updated UUID for player {} to {}",
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
        if (!Files.exists(giftsFile)) return;

        List<String> updatedLines = new ArrayList<>();
        boolean rewarded = false;

        try {
            for (String line : GiftQueueFile.read(giftsFile, 2)) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 3) {
                    updatedLines.add(line);
                    continue;
                }

                String nick = parts[0];
                String reward = parts[1];

                if (nick.equalsIgnoreCase(player.getName())) {
                    Duration duration;
                    try {
                        duration = TimeUtils.parseDuration(reward);
                    } catch (IllegalArgumentException | ArithmeticException e) {
                        log.error("Invalid gift '{}' for player {}", reward, nick);
                        player.sendMessage(Component.text(
                                JoupenProperties.invalidGiftMessage.replace("{reward}", reward),
                                NamedTextColor.RED
                        ));
                        updatedLines.add(line); // оставляем строку
                        continue;
                    }

                    OperationResult result;
                    try {
                        result = operations.grant(nick, duration, OperationType.GIFT,
                                new OperationMetadata(null, "gift-file", null, parts[2]), player.getUniqueId());
                    } catch (RuntimeException e) {
                        log.error("Failed to apply gift for {}", nick, e);
                        updatedLines.add(line);
                        continue;
                    }

                    if (result.applied()) player.sendMessage(Component.text(
                            JoupenProperties.giftAppliedMessage.replace(
                                    "{duration}",
                                    TimeUtils.formatDuration(duration)
                            ),
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
                GiftQueueFile.write(giftsFile, updatedLines);
            } catch (IOException e) {
                log.error("Error writing gifts.txt: {}", e.getMessage());
            }
        }
    }
}
