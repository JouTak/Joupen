package event;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.joupen.bukkit.event.JoupenPassProlongedEvent;
import org.joupen.domain.PlayerEntity;
import org.joupen.repository.PlayerRepository;
import org.joupen.service.PlayerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PlayerProlongBukkitEventTest {

    private ServerMock server;
    private PlayerRepository repo;
    private PlayerService service;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        repo = mock(PlayerRepository.class);
        service = new PlayerService(repo);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void prolongOne_shouldFireJoupenPassProlongedEvent() {
        LocalDateTime now = LocalDateTime.now();
        UUID playerUuid = UUID.randomUUID();
        PlayerEntity entity = new PlayerEntity(1L, playerUuid, "TestPlayer",
                now.plusDays(10), now.minusDays(5), true);

        when(repo.findByName("TestPlayer")).thenReturn(Optional.of(entity));
        doNothing().when(repo).updateByName(any(), anyString());

        AtomicInteger eventCount = new AtomicInteger(0);
        AtomicReference<JoupenPassProlongedEvent> capturedEvent = new AtomicReference<>();

        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPassProlong(JoupenPassProlongedEvent event) {
                eventCount.incrementAndGet();
                capturedEvent.set(event);
            }
        }, MockBukkit.createMockPlugin());

        service.prolongOne("TestPlayer", Duration.ofDays(30), true);

        assertEquals(1, eventCount.get(), "Bukkit event should be fired once");
        assertNotNull(capturedEvent.get(), "Event should be captured");

        JoupenPassProlongedEvent event = capturedEvent.get();
        assertEquals(playerUuid, event.getUuid());
        assertEquals("TestPlayer", event.getName());
        assertTrue(event.isGift());
        assertEquals(Duration.ofDays(30), event.getDuration());
        assertNotNull(event.getValidUntil());
        assertTrue(event.getValidUntil().isAfter(now.plusDays(10)));
    }

    @Test
    void prolongOne_eventShouldContainCorrectData_forNewPlayer() {
        when(repo.findByName("NewPlayer")).thenReturn(Optional.empty());
        doNothing().when(repo).save(any());

        AtomicReference<JoupenPassProlongedEvent> capturedEvent = new AtomicReference<>();

        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPassProlong(JoupenPassProlongedEvent event) {
                capturedEvent.set(event);
            }
        }, MockBukkit.createMockPlugin());

        LocalDateTime before = LocalDateTime.now();
        service.prolongOne("NewPlayer", Duration.ofDays(7), false);

        assertNotNull(capturedEvent.get());
        JoupenPassProlongedEvent event = capturedEvent.get();
        assertEquals("NewPlayer", event.getName());
        assertFalse(event.isGift());
        assertEquals(Duration.ofDays(7), event.getDuration());
        assertTrue(event.getValidUntil().isAfter(before));
    }

    @Test
    void prolongOne_eventShouldReflectExpiredPassExtension() {
        LocalDateTime now = LocalDateTime.now();
        UUID playerUuid = UUID.randomUUID();
        PlayerEntity expired = new PlayerEntity(1L, playerUuid, "ExpiredPlayer",
                now.minusDays(5), now.minusDays(30), true);

        when(repo.findByName("ExpiredPlayer")).thenReturn(Optional.of(expired));
        doNothing().when(repo).updateByName(any(), anyString());

        AtomicReference<JoupenPassProlongedEvent> capturedEvent = new AtomicReference<>();

        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPassProlong(JoupenPassProlongedEvent event) {
                capturedEvent.set(event);
            }
        }, MockBukkit.createMockPlugin());

        service.prolongOne("ExpiredPlayer", Duration.ofDays(15), false);

        assertNotNull(capturedEvent.get());
        JoupenPassProlongedEvent event = capturedEvent.get();
        assertEquals("ExpiredPlayer", event.getName());
        assertTrue(event.getValidUntil().isAfter(now));
        assertTrue(event.getValidUntil().isBefore(now.plusDays(16)));
    }
}
