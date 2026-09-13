package service;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.joupen.domain.OperationMetadata;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.service.GiftQueueFile;
import org.joupen.service.PlayerService;
import org.joupen.service.ScheduledGiftService;
import org.joupen.utils.EventUtils;
import org.joupen.utils.JoupenProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AuditedGiftsTest {
    @TempDir
    Path directory;
    private PlayerRepositoryFileImpl repo;
    private PlayerService service;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        EventUtils.reset();
        JoupenProperties.playersFilepath = directory.resolve("player.json").toString();
        repo = new PlayerRepositoryFileImpl();
        service = new PlayerService(repo);
    }

    @AfterEach
    void tearDown() {
        EventUtils.reset();
        MockBukkit.unmock();
    }

    @Test
    void queueIdsArePersistedBeforeApplyingAndSurviveRestart() throws Exception {
        Path file = directory.resolve("gifts.txt");
        Files.writeString(file, "Player 1d\nPlayer 1d\n");
        var lines = GiftQueueFile.read(file, 2);
        assertNotEquals(lines.get(0), lines.get(1));
        assertEquals(lines, GiftQueueFile.read(file, 2));
    }

    @Test
    void scheduledGiftRetryDoesNotRepeatTheGrant() throws Exception {
        Path file = directory.resolve("scheduled-gifts.txt");
        String line = "Player 3d " + LocalDate.now() + " request-1";
        Files.writeString(file, line);
        var scheduler = new ScheduledGiftService(MockBukkit.createMockPlugin(), service, file);
        var process = ScheduledGiftService.class.getDeclaredMethod("processDueGifts");
        process.setAccessible(true);
        process.invoke(scheduler);
        var validUntil = repo.findByName("Player").orElseThrow().getValidUntil();
        Files.writeString(file, line);
        process.invoke(scheduler);
        assertEquals(validUntil, repo.findByName("Player").orElseThrow().getValidUntil());
        assertEquals(1, repo.findHistory("Player", 10, 0).size());
        assertTrue(Files.readString(file).isBlank());
    }

    @Test
    void notificationFailureDoesNotFailOrRepeatAppliedOperation() {
        EventUtils.register(PlayerProlongedEvent.class, event -> { throw new IllegalStateException("Notification failed"); });
        OperationMetadata metadata = new OperationMetadata(null, "test", null, "request-1");
        assertTrue(service.prolongOne("Player", Duration.ofDays(3), true, metadata).applied());
        assertFalse(service.prolongOne("Player", Duration.ofDays(3), true, metadata).applied());
        assertEquals(1, repo.findHistory("Player", 10, 0).size());
    }
}
