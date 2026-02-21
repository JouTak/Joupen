package service;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.joupen.domain.PlayerEntity;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.repository.PlayerRepository;
import org.joupen.service.PlayerService;
import org.joupen.utils.EventUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PlayerServiceTest {

    private ServerMock server;
    private PlayerRepository repo;
    private PlayerService service;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        EventUtils.reset();
        repo = mock(PlayerRepository.class);
        service = new PlayerService(repo);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void prolongOne_shouldCreateNewIfAbsent_andPublishEvent() {
        when(repo.findByName("Neo")).thenReturn(Optional.empty());

        AtomicInteger events = new AtomicInteger(0);
        EventUtils.register(PlayerProlongedEvent.class, e -> events.incrementAndGet());

        var before = LocalDateTime.now();
        var res = service.prolongOne("Neo", Duration.ofDays(5), false);

        assertEquals("Neo", res.getName());
        assertNotNull(res.getValidUntil());
        assertTrue(res.getValidUntil().isAfter(before));
        assertTrue(Boolean.TRUE.equals(res.getPaid()));

        verify(repo).save(any(PlayerEntity.class));
        assertEquals(1, events.get());
    }

    @Test
    void prolongOne_shouldPublishEventWithCorrectData() {
        LocalDateTime now = LocalDateTime.now();
        PlayerEntity existing = new PlayerEntity(1L, UUID.randomUUID(), "TestPlayer", 
                now.plusDays(10), now.minusDays(20), true);
        
        when(repo.findByName("TestPlayer")).thenReturn(Optional.of(existing));
        doNothing().when(repo).updateByName(any(), anyString());

        AtomicInteger eventCount = new AtomicInteger(0);
        EventUtils.register(PlayerProlongedEvent.class, e -> {
            eventCount.incrementAndGet();
            assertEquals("TestPlayer", e.player().getName());
            assertTrue(e.gift());
            assertEquals(Duration.ofDays(30), e.duration());
            assertNotNull(e.player().getValidUntil());
        });

        var result = service.prolongOne("TestPlayer", Duration.ofDays(30), true);

        assertEquals(1, eventCount.get());
        assertEquals("TestPlayer", result.getName());
        assertTrue(result.getValidUntil().isAfter(now.plusDays(10)));
        verify(repo).updateByName(any(PlayerEntity.class), eq("TestPlayer"));
    }

    @Test
    void prolongOne_shouldExtendFromNowIfExpired() {
        LocalDateTime now = LocalDateTime.now();
        PlayerEntity expired = new PlayerEntity(1L, UUID.randomUUID(), "ExpiredPlayer",
                now.minusDays(5), now.minusDays(30), true);

        when(repo.findByName("ExpiredPlayer")).thenReturn(Optional.of(expired));
        doNothing().when(repo).updateByName(any(), anyString());

        var result = service.prolongOne("ExpiredPlayer", Duration.ofDays(10), false);

        assertNotNull(result.getValidUntil());
        assertTrue(result.getValidUntil().isAfter(now));
        assertTrue(result.getValidUntil().isBefore(now.plusDays(11)));
        verify(repo).updateByName(any(PlayerEntity.class), eq("ExpiredPlayer"));
    }

    @Test
    void prolongOne_shouldExtendFromValidUntilIfActive() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureValidUntil = now.plusDays(15);
        PlayerEntity active = new PlayerEntity(1L, UUID.randomUUID(), "ActivePlayer",
                futureValidUntil, now.minusDays(10), true);

        when(repo.findByName("ActivePlayer")).thenReturn(Optional.of(active));
        doNothing().when(repo).updateByName(any(), anyString());

        var result = service.prolongOne("ActivePlayer", Duration.ofDays(30), false);

        assertNotNull(result.getValidUntil());
        assertTrue(result.getValidUntil().isAfter(futureValidUntil));
        assertTrue(result.getValidUntil().isBefore(futureValidUntil.plusDays(31)));
        verify(repo).updateByName(any(PlayerEntity.class), eq("ActivePlayer"));
    }

    @Test
    void prolongAll_shouldSkipUnpaid_ifNotGift() {
        PlayerEntity paid = new PlayerEntity(1L, UUID.randomUUID(), "Paid", LocalDateTime.now(), LocalDateTime.now(), true);
        PlayerEntity unpaid = new PlayerEntity(2L, UUID.randomUUID(), "Unpaid", LocalDateTime.now(), LocalDateTime.now(), false);

        when(repo.findAll()).thenReturn(List.of(paid, unpaid));

        when(repo.findByName("Paid")).thenReturn(Optional.of(paid));
        verify(repo, never()).findByName("Unpaid");

        doNothing().when(repo).updateByName(any(), anyString());
        service.prolongAll(Duration.ofDays(3), false);

        verify(repo, atLeastOnce()).updateByName(argThat(p -> p.getName().equals("Paid")), eq("Paid"));
        verify(repo, never()).save(any());
        verify(repo, never()).updateByName(argThat(p -> p.getName().equals("Unpaid")), eq("Unpaid"));
    }

    @Test
    void prolongAll_shouldIncludeUnpaid_ifGift() {
        PlayerEntity paid = new PlayerEntity(1L, UUID.randomUUID(), "Paid", LocalDateTime.now(), LocalDateTime.now(), true);
        PlayerEntity unpaid = new PlayerEntity(2L, UUID.randomUUID(), "Unpaid", LocalDateTime.now(), LocalDateTime.now(), false);

        when(repo.findAll()).thenReturn(List.of(paid, unpaid));

        when(repo.findByName("Paid")).thenReturn(Optional.of(paid));
        when(repo.findByName("Unpaid")).thenReturn(Optional.of(unpaid));

        doNothing().when(repo).updateByName(any(), anyString());

        service.prolongAll(Duration.ofDays(7), true);

        verify(repo, atLeastOnce()).updateByName(argThat(p -> p.getName().equals("Paid")), eq("Paid"));
        verify(repo, atLeastOnce()).updateByName(argThat(p -> p.getName().equals("Unpaid")), eq("Unpaid"));

        verify(repo, never()).save(any());
    }
}
