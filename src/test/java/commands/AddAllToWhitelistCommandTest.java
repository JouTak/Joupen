package commands;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.impl.AddAllToWhitelistCommand;
import org.joupen.domain.PlayerEntity;
import org.joupen.messaging.Messaging;
import org.joupen.messaging.Recipient;
import org.joupen.messaging.channels.MessageChannel;
import org.joupen.repository.PlayerRepository;
import org.joupen.service.PlayerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AddAllToWhitelistCommandTest {

    private ServerMock server;
    private PlayerRepository repo;
    private PlayerService playerService;
    private CommandSender sender;
    private List<String> messages;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        repo = mock(PlayerRepository.class);
        playerService = new PlayerService(repo);
        sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("Admin");
        messages = new ArrayList<>();

        try {
            var m = Messaging.class.getDeclaredMethod("resetForTests");
            m.setAccessible(true);
            m.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Messaging.registerChannel(new MessageChannel() {
            @Override
            public String id() {
                return "chat";
            }

            @Override
            public void send(Recipient recipient, Component message) {
                messages.add(PlainTextComponentSerializer.plainText().serialize(message));
            }
        });
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void execute_validFile_shouldImportPlayers() throws IOException {
        Path testFile = tempDir.resolve("players.txt");
        Files.writeString(testFile, "Player1\nPlayer2\nPlayer3");

        when(repo.findByName(anyString())).thenReturn(Optional.empty());
        doNothing().when(repo).save(any(PlayerEntity.class));

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{testFile.toString(), "30"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        AddAllToWhitelistCommand cmd = new AddAllToWhitelistCommand(ctx);
        cmd.execute();

        verify(repo, times(3)).save(any(PlayerEntity.class));
        assertFalse(messages.isEmpty());
        assertTrue(messages.get(0).contains("3"));
    }

    @Test
    void execute_emptyLines_shouldSkip() throws IOException {
        Path testFile = tempDir.resolve("players_with_empty.txt");
        Files.writeString(testFile, "Player1\n\n  \nPlayer2\n");

        when(repo.findByName(anyString())).thenReturn(Optional.empty());
        doNothing().when(repo).save(any(PlayerEntity.class));

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{testFile.toString(), "7"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        AddAllToWhitelistCommand cmd = new AddAllToWhitelistCommand(ctx);
        cmd.execute();

        verify(repo, times(2)).save(any(PlayerEntity.class));
    }

    @Test
    void execute_existingPlayers_shouldSkip() throws IOException {
        Path testFile = tempDir.resolve("existing.txt");
        Files.writeString(testFile, "ExistingPlayer\nNewPlayer");

        when(repo.findByName("ExistingPlayer")).thenReturn(Optional.of(new PlayerEntity()));
        when(repo.findByName("NewPlayer")).thenReturn(Optional.empty());
        doNothing().when(repo).save(any(PlayerEntity.class));

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{testFile.toString(), "15"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        AddAllToWhitelistCommand cmd = new AddAllToWhitelistCommand(ctx);
        cmd.execute();

        verify(repo, times(1)).save(any(PlayerEntity.class));
        assertTrue(messages.get(0).contains("1"));
    }

    @Test
    void execute_nonExistentFile_shouldShowError() {
        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"/non/existent/file.txt", "10"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        AddAllToWhitelistCommand cmd = new AddAllToWhitelistCommand(ctx);
        cmd.execute();

        assertFalse(messages.isEmpty());
        assertTrue(messages.get(0).toLowerCase().contains("ошибка"));
    }

    @Test
    void execute_invalidDaysFormat_shouldThrowException() {
        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"some_file.txt", "invalid"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        AddAllToWhitelistCommand cmd = new AddAllToWhitelistCommand(ctx);
        assertThrows(NumberFormatException.class, cmd::execute);
    }

    @Test
    void execute_relativePath_shouldResolveCorrectly() throws IOException {
        Path testFile = tempDir.resolve("relative.txt");
        Files.writeString(testFile, "Player1");

        when(repo.findByName(anyString())).thenReturn(Optional.empty());
        doNothing().when(repo).save(any(PlayerEntity.class));

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{testFile.getFileName().toString(), "20"})
                .playerRepository(repo)
                .playerService(playerService)
                .build();

        AddAllToWhitelistCommand cmd = new AddAllToWhitelistCommand(ctx);
        assertDoesNotThrow(cmd::execute);
    }
}
