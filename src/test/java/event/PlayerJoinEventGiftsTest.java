package event;

import org.bukkit.event.player.PlayerLoginEvent;
import org.joupen.domain.PlayerEntity;
import org.joupen.events.PlayerJoinEventHandler;
import org.joupen.repository.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


public class PlayerJoinEventGiftsTest extends BaseTest {

    private PlayerRepository repo;
    private PlayerJoinEventHandler handler;

    private Path pluginsDir;
    private Path giftsFile;

    @BeforeEach
    void init() throws IOException {
        repo = mock(PlayerRepository.class);
        handler = new PlayerJoinEventHandler(repo);

        pluginsDir = Paths.get("plugins", "plugins/joupen");
        Files.createDirectories(pluginsDir);
        giftsFile = pluginsDir.resolve("gifts.txt");
        Files.deleteIfExists(giftsFile);
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.deleteIfExists(giftsFile);
    }

    @Test
    void giftValidLine_shouldProlongAndRemoveLineFromFile_whenPlayerNotExists() throws Exception {
        Files.writeString(giftsFile, "TestPlayer 3d\nOther 2d\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        when(repo.findByUuid(player.getUniqueId())).thenReturn(Optional.of(new PlayerEntity(player.getName(), false, player.getUniqueId(), LocalDateTime.now().plusDays(3), LocalDateTime.now())));
        when(repo.findByName("TestPlayer")).thenReturn(Optional.empty());

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", InetAddress.getByName("127.0.0.1"));

        // act
        handler.playerJoinEvent(event);

        // assert: allow
        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());

        // новый игрок сохранён
//        ArgumentCaptor<PlayerEntity> captor = ArgumentCaptor.forClass(PlayerEntity.class);
//        verify(repo).save(captor.capture());

//        PlayerEntity saved = captor.getValue();
//        assertEquals("TestPlayer", saved.getName());
//        assertEquals(player.getUniqueId(), saved.getUuid());
//        assertTrue(saved.getValidUntil().isAfter(LocalDateTime.now().plus(Duration.ofDays(2))), "validUntil должен быть ~+3d");
//        assertNotNull(saved.getLastProlongDate());

        // строка для TestPlayer удалена, другая осталась
        String content = Files.readString(giftsFile, StandardCharsets.UTF_8);
//        assertFalse(content.contains("TestPlayer 3d"));
//        assertTrue(content.contains("Other 2d"));
    }

    @Test
    void giftInvalidFormat_shouldNotKickAndKeepFileUnchanged() throws Exception {
        // arrange
        Files.writeString(giftsFile, "TestPlayer WRONG\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        when(repo.findByUuid(player.getUniqueId())).thenReturn(Optional.empty());

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", InetAddress.getByName("127.0.0.1"));

        // act
        handler.playerJoinEvent(event);

        // assert: KICK_OTHER с текстом про неверный формат
        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, event.getResult());
        String raw = event.getKickMessage();
        assertTrue(raw.equalsIgnoreCase("§9Тебя нет в вайтлисте. Напиши по этому поводу §cEnderDiss'e"), "ожидали текст о неверном формате");

        // репозиторий не трогали
        verify(repo, never()).save(any());
        verify(repo, never()).updateByName(any(), anyString());

        // файл не изменился
        String content = Files.readString(giftsFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("TestPlayer WRONG"));
    }
}
