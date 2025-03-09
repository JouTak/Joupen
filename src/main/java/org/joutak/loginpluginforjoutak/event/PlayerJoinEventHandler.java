package org.joutak.loginpluginforjoutak.event;

import jakarta.transaction.Transactional;
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
import org.joutak.loginpluginforjoutak.dto.PlayerDtos;
import org.joutak.loginpluginforjoutak.inputoutput.JsonReaderImpl;
import org.joutak.loginpluginforjoutak.inputoutput.JsonWriterImpl;
import org.joutak.loginpluginforjoutak.inputoutput.Reader;
import org.joutak.loginpluginforjoutak.inputoutput.Writer;
import org.joutak.loginpluginforjoutak.mapper.PlayerMapper;
import org.joutak.loginpluginforjoutak.repository.PlayerDtosUtils;
import org.joutak.loginpluginforjoutak.repository.PlayerRepository;
import org.joutak.loginpluginforjoutak.utils.JoutakProperties;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class PlayerJoinEventHandler implements Listener {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private static final UUID INITIAL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public PlayerJoinEventHandler(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
    }

    @EventHandler
    @Transactional
    public void playerJoinEvent(PlayerLoginEvent playerLoginEvent) {
        Player player = playerLoginEvent.getPlayer();
        PlayerEntity playerEntity = null;
        PlayerDto playerDto = null;

        // Пытаемся получить данные из репозитория
        try {
            Optional<PlayerEntity> optionalEntity = playerRepository.findByUuid(player.getUniqueId());
            playerEntity = optionalEntity.orElse(null);
            if (playerEntity == null) {
                // Если не нашли по UUID, ищем по имени
                optionalEntity = playerRepository.findByName(player.getName());
                playerEntity = optionalEntity.orElse(null);
            }
        } catch (Exception e) {
            log.warn("Ошибка доступа к репозиторию для игрока {}. Переключаюсь на файл.", player.getName(), e);
        }

        // Если данные из репозитория не получены, используем файл
        if (playerEntity == null) {
            playerDto = PlayerDtosUtils.findPlayerByUuid(playerLoginEvent.getPlayer().getUniqueId());
            if (playerDto == null) {
                playerDto = PlayerDtosUtils.findPlayerByName(playerLoginEvent.getPlayer().getName());
            }

            if (playerDto != null && !playerDto.getName().equals(playerLoginEvent.getPlayer().getName())) {
                updatePlayerInFile(playerDto, playerLoginEvent.getPlayer().getName(), playerDto.getUuid());
                log.warn("Player {} updated his nickname, adjusted in file.", playerDto.getName());
            }
        } else {
            // Конвертируем PlayerEntity в PlayerDto для дальнейшей логики
            playerDto = playerMapper.entityToDto(playerEntity);
        }

        // Проверка вайтлиста
        if (playerDto == null) {
            TextComponent textComponent = Component.text()
                    .append(Component.text("Тебя нет в вайтлисте. Напиши по этому поводу ", NamedTextColor.BLUE))
                    .append(Component.text("EnderDiss'e", NamedTextColor.RED))
                    .build();
            playerLoginEvent.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, textComponent);
            return;
        }

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
        if (uuid.equals(INITIAL_UUID)) {
            LocalDate now = LocalDate.now();
            LocalDate validUntil = playerDto.getValidUntil()
                    .plusDays(ChronoUnit.DAYS.between(playerDto.getLastProlongDate(), now));
            playerDto.setValidUntil(validUntil);
            playerDto.setLastProlongDate(now);
            playerDto.setUuid(playerLoginEvent.getPlayer().getUniqueId());

            // Пытаемся сохранить в репозиторий
            try {
                if (playerEntity == null) {
                    playerRepository.save(playerDto); // Создание новой записи
                } else {
                    playerRepository.update(playerDto); // Обновление существующей
                }
            } catch (Exception e) {
                log.warn("Ошибка сохранения в репозитории для игрока {}. Сохраняю в файл.", playerDto.getName(), e);
                updatePlayerInFile(playerDto, player.getName(), INITIAL_UUID);
            }
            log.warn("Player {} joined for the first time, adjusted prohodka and changed UUID", playerDto.getName());
        }

        playerLoginEvent.allow();
    }

    private void updatePlayerInFile(PlayerDto playerDto, String newName, UUID searchUuid) {
        Writer writer = new JsonWriterImpl(JoutakProperties.saveFilepath);
        Reader reader = new JsonReaderImpl(JoutakProperties.saveFilepath);

        PlayerDtos playerDtos = reader.read();
        if (playerDtos == null || playerDtos.getPlayerDtoList() == null) {
            log.error("Player data file is empty or corrupted at {}", JoutakProperties.saveFilepath);
            return;
        }

        List<PlayerDto> list = playerDtos.getPlayerDtoList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getUuid().equals(searchUuid)) {
                playerDto.setName(newName); // Обновляем имя
                list.set(i, playerDto);     // Заменяем весь объект
                writer.write(playerDtos);    // Записываем обновлённые данные
                return;
            }
        }
        log.error("Player with UUID {} not found in file for update!", searchUuid);
    }
}