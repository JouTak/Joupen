package commands;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.impl.GiftCommand;
import org.joupen.commands.impl.ProlongCommand;
import org.joupen.domain.PlayerEntity;
import org.joupen.repository.PlayerRepository;
import org.joupen.service.PlayerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ProlongCommandTest {

    private ServerMock server;
    private PlayerRepository repo;
    private PlayerService playerService;
    private CommandSender sender;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        repo = mock(PlayerRepository.class);
        playerService = new PlayerService(repo);
        sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("Admin");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void execute_prolongSinglePlayer_shouldCallService() {
        when(repo.findByName("TestPlayer")).thenReturn(Optional.of(
                new PlayerEntity(1L, UUID.randomUUID(), "TestPlayer",
                        LocalDateTime.now().plusDays(10), LocalDateTime.now(), true)));
        doNothing().when(repo).updateByName(any(), anyString());

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"TestPlayer", "30d"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        ProlongCommand cmd = new ProlongCommand(ctx);
        cmd.execute();

        verify(repo, times(1)).updateByName(any(PlayerEntity.class), eq("TestPlayer"));
    }

    @Test
    void execute_prolongAll_shouldProcessAllPaidPlayers() {
        PlayerEntity paid1 = new PlayerEntity(1L, UUID.randomUUID(), "Paid1",
                LocalDateTime.now(), LocalDateTime.now(), true);
        PlayerEntity paid2 = new PlayerEntity(2L, UUID.randomUUID(), "Paid2",
                LocalDateTime.now(), LocalDateTime.now(), true);
        PlayerEntity unpaid = new PlayerEntity(3L, UUID.randomUUID(), "Unpaid",
                LocalDateTime.now(), LocalDateTime.now(), false);

        when(repo.findAll()).thenReturn(List.of(paid1, paid2, unpaid));
        when(repo.findByName("Paid1")).thenReturn(Optional.of(paid1));
        when(repo.findByName("Paid2")).thenReturn(Optional.of(paid2));
        doNothing().when(repo).updateByName(any(), anyString());

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"all", "15d"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        ProlongCommand cmd = new ProlongCommand(ctx);
        cmd.execute();

        verify(repo, times(1)).updateByName(argThat(p -> p.getName().equals("Paid1")), eq("Paid1"));
        verify(repo, times(1)).updateByName(argThat(p -> p.getName().equals("Paid2")), eq("Paid2"));
        verify(repo, never()).updateByName(argThat(p -> p.getName().equals("Unpaid")), eq("Unpaid"));
    }

    @Test
    void execute_defaultDuration_shouldUse30Days() {
        when(repo.findByName("TestPlayer")).thenReturn(Optional.of(
                new PlayerEntity(1L, UUID.randomUUID(), "TestPlayer",
                        LocalDateTime.now().plusDays(10), LocalDateTime.now(), true)));
        doNothing().when(repo).updateByName(any(), anyString());

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"TestPlayer"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        ProlongCommand cmd = new ProlongCommand(ctx);
        cmd.execute();

        verify(repo, times(1)).updateByName(any(PlayerEntity.class), eq("TestPlayer"));
    }

    @Test
    void execute_customDuration_shouldParseDuration() {
        when(repo.findByName("TestPlayer")).thenReturn(Optional.of(
                new PlayerEntity(1L, UUID.randomUUID(), "TestPlayer",
                        LocalDateTime.now().plusDays(10), LocalDateTime.now(), true)));
        doNothing().when(repo).updateByName(any(), anyString());

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"TestPlayer", "2mo5d"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        ProlongCommand cmd = new ProlongCommand(ctx);
        cmd.execute();

        verify(repo, times(1)).updateByName(any(PlayerEntity.class), eq("TestPlayer"));
    }

    @Test
    void giftCommand_shouldSetGiftFlag() {
        PlayerEntity unpaid = new PlayerEntity(1L, UUID.randomUUID(), "UnpaidPlayer",
                LocalDateTime.now().minusDays(5), LocalDateTime.now(), false);

        when(repo.findByName("UnpaidPlayer")).thenReturn(Optional.of(unpaid));
        doNothing().when(repo).updateByName(any(), anyString());

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"UnpaidPlayer", "7d"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        GiftCommand cmd = new GiftCommand(ctx);
        cmd.execute();

        verify(repo, times(1)).updateByName(any(PlayerEntity.class), eq("UnpaidPlayer"));
    }

    @Test
    void giftCommand_prolongAll_shouldIncludeUnpaid() {
        PlayerEntity paid = new PlayerEntity(1L, UUID.randomUUID(), "Paid",
                LocalDateTime.now(), LocalDateTime.now(), true);
        PlayerEntity unpaid = new PlayerEntity(2L, UUID.randomUUID(), "Unpaid",
                LocalDateTime.now(), LocalDateTime.now(), false);

        when(repo.findAll()).thenReturn(List.of(paid, unpaid));
        when(repo.findByName("Paid")).thenReturn(Optional.of(paid));
        when(repo.findByName("Unpaid")).thenReturn(Optional.of(unpaid));
        doNothing().when(repo).updateByName(any(), anyString());

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"all", "10d"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        GiftCommand cmd = new GiftCommand(ctx);
        cmd.execute();

        verify(repo, times(1)).updateByName(argThat(p -> p.getName().equals("Paid")), eq("Paid"));
        verify(repo, times(1)).updateByName(argThat(p -> p.getName().equals("Unpaid")), eq("Unpaid"));
    }
}
