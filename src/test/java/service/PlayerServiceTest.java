package service;

import org.joupen.domain.PlayerEntity;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.repository.PlayerRepository;
import org.joupen.service.PlayerService;
import org.joupen.utils.EventUtils;
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

    private PlayerRepository repo;
    private PlayerService service;

    @BeforeEach
    void setUp() {
        EventUtils.reset();
        repo = mock(PlayerRepository.class);
        service = new PlayerService(repo);
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
        assertTrue(Boolean.TRUE.equals(res.getPaid())); // gift=false => paid=true в заготовке сущности

        verify(repo).save(any(PlayerEntity.class));
        assertEquals(1, events.get());
    }

    // ... imports и setUp() без изменений

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
