package event;

import org.bukkit.event.player.PlayerLoginEvent;
import org.joupen.domain.PlayerEntity;
import org.joupen.events.PlayerJoinEventHandler;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.utils.JoupenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerJoinEventGiftsTest extends BaseTest {
    @TempDir
    Path directory;
    private PlayerRepositoryFileImpl repo;
    private PlayerJoinEventHandler handler;
    private Path giftsFile;

    @BeforeEach
    void init() {
        JoupenProperties.playersFilepath = directory.resolve("player.json").toString();
        repo = new PlayerRepositoryFileImpl();
        giftsFile = directory.resolve("gifts.txt");
        handler = new PlayerJoinEventHandler(repo, giftsFile);
    }

    @Test
    void giftIsAuditedAndRemovedWithoutGrantingApproval() throws Exception {
        Files.writeString(giftsFile, "TestPlayer 3d\nOther 2d\n");

        PlayerLoginEvent event = login();

        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, event.getResult());
        assertFalse(repo.findByName("TestPlayer").orElseThrow().getApproved());
        assertEquals(player.getUniqueId(), repo.findByName("TestPlayer").orElseThrow().getUuid());
        assertEquals(1, repo.findHistory("TestPlayer", 10, 0).size());
        assertNotNull(repo.findHistory("TestPlayer", 10, 0).get(0).getMetadata().externalId());
        assertFalse(Files.readString(giftsFile).contains("TestPlayer"));
        assertTrue(Files.readString(giftsFile).contains("Other 2d"));
    }

    @Test
    void replayedGiftDoesNotProlongTwice() throws Exception {
        PlayerEntity entity = new PlayerEntity(player.getName(), false, player.getUniqueId(),
                LocalDateTime.now().plusDays(1).withNano(0), LocalDateTime.now().withNano(0));
        entity.setApproved(true);
        repo.save(entity);
        Files.writeString(giftsFile, "TestPlayer 3d gift-123");

        assertEquals(PlayerLoginEvent.Result.ALLOWED, login().getResult());
        LocalDateTime validUntil = repo.findByName("TestPlayer").orElseThrow().getValidUntil();
        Files.writeString(giftsFile, "TestPlayer 3d gift-123");
        assertEquals(PlayerLoginEvent.Result.ALLOWED, login().getResult());

        assertEquals(validUntil, repo.findByName("TestPlayer").orElseThrow().getValidUntil());
        assertEquals(1, repo.findHistory("TestPlayer", 10, 0).size());
        assertTrue(Files.readString(giftsFile).isBlank());
    }

    @Test
    void invalidGiftIsRetainedWithoutChangingAccess() throws Exception {
        Files.writeString(giftsFile, "TestPlayer WRONG");

        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, login().getResult());
        assertTrue(repo.findAll().isEmpty());
        assertTrue(Files.readString(giftsFile).contains("TestPlayer WRONG"));
    }

    private PlayerLoginEvent login() throws Exception {
        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", InetAddress.getLoopbackAddress());
        handler.playerJoinEvent(event);
        return event;
    }
}
