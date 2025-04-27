package event;

import org.bukkit.event.player.PlayerLoginEvent;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.event.PlayerJoinEventHandler;
import org.joutak.loginpluginforjoutak.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PlayerJoinEventTest extends BaseTest {

    private PlayerJoinEventHandler playerJoinEventHandler;
    private PlayerRepository playerRepository;

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        playerJoinEventHandler = new PlayerJoinEventHandler(playerRepository);
    }

    @Test
    void playerNotInDatabaseOrFile_ShouldKickWithWhitelistMessage() {
        String newUnknownName = "UnknownPlayer";
        player.setName(newUnknownName);
        UUID playerUuid = player.getUniqueId();

        when(playerRepository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        when(playerRepository.findByName(newUnknownName)).thenReturn(Optional.empty());

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", Objects.requireNonNull(player.getAddress()).getAddress());
        playerJoinEventHandler.playerJoinEvent(event);

        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, event.getResult());
        assertTrue(event.getKickMessage().contains("Тебя нет в вайтлисте"));

        verify(playerRepository).findByUuid(playerUuid);
        verify(playerRepository).findByName(newUnknownName);
        verifyNoMoreInteractions(playerRepository);
    }

    @Test
    void playerInDatabaseWithExpiredSubscription_ShouldKickWithExpiredMessage() {
        UUID playerUuid = player.getUniqueId();
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setUuid(playerUuid);
        playerEntity.setName(TEST_NAME);
        playerEntity.setValidUntil(LocalDateTime.now().minusDays(1));
        playerEntity.setLastProlongDate(LocalDateTime.now().minusDays(30));
        playerEntity.setPaid(true);

        when(playerRepository.findByUuid(playerUuid)).thenReturn(Optional.of(playerEntity));

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", Objects.requireNonNull(player.getAddress()).getAddress());
        playerJoinEventHandler.playerJoinEvent(event);

        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, event.getResult());
        assertTrue(event.getKickMessage().contains("Проходка кончилась"));

        verify(playerRepository).findByUuid(playerUuid);
        verifyNoMoreInteractions(playerRepository);
    }

    @Test
    void firstLoginFromDatabase_ShouldUpdateUuidAndSubscription() {
        UUID playerUuid = player.getUniqueId();
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setUuid(TEST_UUID);
        playerEntity.setName(TEST_NAME);

        LocalDateTime lastProlongDate = LocalDateTime.now().minusDays(20).withSecond(0).withNano(0);
        LocalDateTime validUntil = LocalDateTime.now().plusDays(10).withSecond(0).withNano(0);
        playerEntity.setValidUntil(validUntil);
        playerEntity.setLastProlongDate(lastProlongDate);
        playerEntity.setPaid(true);

        when(playerRepository.findByUuid(playerUuid)).thenReturn(Optional.of(playerEntity));

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", Objects.requireNonNull(player.getAddress()).getAddress());
        playerJoinEventHandler.playerJoinEvent(event);

        ArgumentCaptor<PlayerDto> captor = ArgumentCaptor.forClass(PlayerDto.class);
        verify(playerRepository).update(captor.capture());
        PlayerDto updatedDto = captor.getValue();

        assertEquals(playerUuid, updatedDto.getUuid());

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastProlongDate.toLocalDate(), LocalDateTime.now().toLocalDate());
        assertEquals(validUntil.plusDays(daysBetween), updatedDto.getValidUntil());

        // Проверка только даты, без учёта времени
        assertEquals(LocalDateTime.now().withSecond(0).withNano(0).withHour(0).withMinute(0),
                updatedDto.getLastProlongDate().withHour(0).withMinute(0).withSecond(0).withNano(0));

        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());

        verify(playerRepository).findByUuid(playerUuid);
        verifyNoMoreInteractions(playerRepository);
    }

    @Test
    void validPlayerInDatabase_ShouldAllowLogin() {
        UUID playerUuid = player.getUniqueId();
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setUuid(playerUuid);
        playerEntity.setName(TEST_NAME);
        playerEntity.setValidUntil(LocalDateTime.now().plusDays(10));
        playerEntity.setLastProlongDate(LocalDateTime.now().minusDays(20));
        playerEntity.setPaid(true);

        when(playerRepository.findByUuid(playerUuid)).thenReturn(Optional.of(playerEntity));

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", Objects.requireNonNull(player.getAddress()).getAddress());
        playerJoinEventHandler.playerJoinEvent(event);

        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());

        verify(playerRepository).findByUuid(playerUuid);
        verifyNoMoreInteractions(playerRepository);
    }
}
