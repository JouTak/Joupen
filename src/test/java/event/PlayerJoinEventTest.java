package event;

import org.bukkit.event.player.PlayerLoginEvent;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.dto.PlayerDtos;
import org.joutak.loginpluginforjoutak.event.PlayerJoinEventHandler;
import org.joutak.loginpluginforjoutak.inputoutput.JsonReaderImpl;
import org.joutak.loginpluginforjoutak.inputoutput.JsonWriterImpl;
import org.joutak.loginpluginforjoutak.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        playerEntity.setValidUntil(LocalDate.now().minusDays(1));
        playerEntity.setLastProlongDate(LocalDate.now().minusDays(30));

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
        LocalDate lastProlongDate = LocalDate.now().minusDays(20);
        LocalDate validUntil = LocalDate.now().plusDays(10);
        playerEntity.setValidUntil(validUntil);
        playerEntity.setLastProlongDate(lastProlongDate);

        when(playerRepository.findByUuid(playerUuid)).thenReturn(Optional.of(playerEntity));

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", Objects.requireNonNull(player.getAddress()).getAddress());
        playerJoinEventHandler.playerJoinEvent(event);

        ArgumentCaptor<PlayerDto> captor = ArgumentCaptor.forClass(PlayerDto.class);
        verify(playerRepository).update(captor.capture());
        PlayerDto updatedDto = captor.getValue();

        assertEquals(playerUuid, updatedDto.getUuid());
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastProlongDate, LocalDate.now());
        assertEquals(validUntil.plusDays(daysBetween), updatedDto.getValidUntil());
        assertEquals(LocalDate.now(), updatedDto.getLastProlongDate());

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
        playerEntity.setValidUntil(LocalDate.now().plusDays(10));
        playerEntity.setLastProlongDate(LocalDate.now().minusDays(20));

        when(playerRepository.findByUuid(playerUuid)).thenReturn(Optional.of(playerEntity));

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", Objects.requireNonNull(player.getAddress()).getAddress());
        playerJoinEventHandler.playerJoinEvent(event);

        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());

        verify(playerRepository).findByUuid(playerUuid);
        verifyNoMoreInteractions(playerRepository);
    }

    @Test
    void databaseErrorPlayerInFileWithOldName_ShouldUpdateNicknameInFile() {
        UUID playerUuid = player.getUniqueId();
        PlayerDto filePlayerDto = new PlayerDto();
        filePlayerDto.setUuid(playerUuid);
        filePlayerDto.setName("OldName");
        filePlayerDto.setValidUntil(LocalDate.now().plusDays(10));
        filePlayerDto.setLastProlongDate(LocalDate.now().minusDays(20));

        PlayerDtos playerDtos = new PlayerDtos();
        playerDtos.setPlayerDtoList(List.of(filePlayerDto));

        JsonWriterImpl writer = new JsonWriterImpl(TEST_FILE_PATH);
        writer.write(playerDtos);

        when(playerRepository.findByUuid(playerUuid)).thenThrow(new RuntimeException("DB error"));

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", Objects.requireNonNull(player.getAddress()).getAddress());
        playerJoinEventHandler.playerJoinEvent(event);

        JsonReaderImpl reader = new JsonReaderImpl(TEST_FILE_PATH);
        PlayerDtos updatedDtos = reader.read();
        PlayerDto updatedPlayerDto = updatedDtos.getPlayerDtoList().get(0);

        assertEquals(TEST_NAME, updatedPlayerDto.getName());
        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());

        verify(playerRepository).findByUuid(playerUuid);
        verifyNoMoreInteractions(playerRepository);
    }

    @Test
    void firstLoginWithDatabaseError_ShouldUpdateUuidAndSaveToFile() {
        UUID playerUuid = player.getUniqueId();
        PlayerDto filePlayerDto = new PlayerDto();
        filePlayerDto.setUuid(TEST_UUID);
        filePlayerDto.setName(TEST_NAME);
        LocalDate lastProlongDate = LocalDate.now().minusDays(20);
        LocalDate validUntil = LocalDate.now().plusDays(10);
        filePlayerDto.setValidUntil(validUntil);
        filePlayerDto.setLastProlongDate(lastProlongDate);

        PlayerDtos playerDtos = new PlayerDtos();
        playerDtos.setPlayerDtoList(List.of(filePlayerDto));

        JsonWriterImpl writer = new JsonWriterImpl(TEST_FILE_PATH);
        writer.write(playerDtos);

        when(playerRepository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        when(playerRepository.findByName(TEST_NAME)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("DB save error")).when(playerRepository).save(any(PlayerDto.class));

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", Objects.requireNonNull(player.getAddress()).getAddress());
        playerJoinEventHandler.playerJoinEvent(event);

        JsonReaderImpl reader = new JsonReaderImpl(TEST_FILE_PATH);
        PlayerDtos updatedDtos = reader.read();
        PlayerDto updatedPlayerDto = updatedDtos.getPlayerDtoList().get(0);

        assertEquals(playerUuid, updatedPlayerDto.getUuid());
        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());

        verify(playerRepository).findByUuid(playerUuid);
        verify(playerRepository).findByName(TEST_NAME);
        verify(playerRepository).save(any(PlayerDto.class));
        verifyNoMoreInteractions(playerRepository);
    }
}
