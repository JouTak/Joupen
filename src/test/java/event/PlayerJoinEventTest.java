package event;

import org.bukkit.event.player.PlayerLoginEvent;
import org.joupen.database.TransactionManager;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.event.PlayerJoinEventHandler;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jooq.DSLContext;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.joupen.enums.UUIDTypes.INITIAL_UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlayerJoinEventTest extends BaseTest {

    private PlayerJoinEventHandler playerJoinEventHandler;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TransactionManager transactionManager;
    @Mock
    private PlayerMapper playerMapper;
    @Mock
    private DSLContext dslContext;

    @BeforeEach
    void setUp() {
        playerMapper = Mappers.getMapper(PlayerMapper.class);
        playerJoinEventHandler = new PlayerJoinEventHandler(playerRepository, transactionManager);
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
        verifyNoInteractions(transactionManager);
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

        PlayerDto playerDto = playerMapper.entityToDto(playerEntity);

        when(playerRepository.findByUuid(playerUuid)).thenReturn(Optional.of(playerEntity));

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", Objects.requireNonNull(player.getAddress()).getAddress());
        playerJoinEventHandler.playerJoinEvent(event);

        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, event.getResult());
        assertTrue(event.getKickMessage().contains("Проходка кончилась"));

        verify(playerRepository).findByUuid(playerUuid);
        verifyNoInteractions(transactionManager);
        verifyNoMoreInteractions(playerRepository);
    }

    @Test
    void firstLoginFromDatabase_ShouldUpdateUuidAndSubscription() {
        UUID playerUuid = player.getUniqueId();
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setUuid(INITIAL_UUID.getUuid());
        playerEntity.setName(TEST_NAME);

        LocalDateTime lastProlongDate = LocalDateTime.now().minusDays(20).withSecond(0).withNano(0);
        LocalDateTime validUntil = LocalDateTime.now().plusDays(10).withSecond(0).withNano(0);
        playerEntity.setValidUntil(validUntil);
        playerEntity.setLastProlongDate(lastProlongDate);
        playerEntity.setPaid(true);

        PlayerDto playerDto = playerMapper.entityToDto(playerEntity);

        when(playerRepository.findByUuid(playerUuid)).thenReturn(Optional.empty());
        when(playerRepository.findByName(TEST_NAME)).thenReturn(Optional.of(playerEntity));

        doAnswer(invocation -> {
            Consumer<DSLContext> consumer = invocation.getArgument(0);
            consumer.accept(dslContext);
            return null;
        }).when(transactionManager).executeInTransaction(any(Consumer.class));

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", Objects.requireNonNull(player.getAddress()).getAddress());
        playerJoinEventHandler.playerJoinEvent(event);

        ArgumentCaptor<PlayerDto> captor = ArgumentCaptor.forClass(PlayerDto.class);
        verify(playerRepository).update(captor.capture());
        PlayerDto updatedDto = captor.getValue();

        assertEquals(playerUuid, updatedDto.getUuid());
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastProlongDate, LocalDateTime.now());
        assertEquals(validUntil.plusDays(daysBetween).withSecond(0).withNano(0),
                updatedDto.getValidUntil().withSecond(0).withNano(0));
        assertEquals(LocalDateTime.now().withSecond(0).withNano(0),
                updatedDto.getLastProlongDate().withSecond(0).withNano(0));

        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());

        verify(playerRepository).findByUuid(playerUuid);
        verify(playerRepository).findByName(TEST_NAME);
        verify(transactionManager).executeInTransaction(any(Consumer.class));
        verifyNoMoreInteractions(playerRepository, transactionManager);
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
        verifyNoInteractions(transactionManager);
        verifyNoMoreInteractions(playerRepository);
    }
}