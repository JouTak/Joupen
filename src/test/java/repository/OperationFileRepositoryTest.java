package repository;

import org.joupen.domain.OperationMetadata;
import org.joupen.domain.OperationResult;
import org.joupen.domain.OperationType;
import org.joupen.domain.PlayerEntity;
import org.joupen.domain.PlayerOperation;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.utils.JoupenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class OperationFileRepositoryTest {
    @TempDir
    Path directory;
    private PlayerRepositoryFileImpl repo;
    private final OperationMetadata metadata = new OperationMetadata("Test gift", "test", "Admin", "gift-1");

    @BeforeEach
    void setUp() {
        JoupenProperties.playersFilepath = directory.resolve("player.json").toString();
        repo = new PlayerRepositoryFileImpl();
    }

    @Test
    void operationAndPlayer_surviveRestartAndDuplicateRequest() {
        OperationResult first = apply(repo);
        PlayerRepositoryFileImpl restarted = new PlayerRepositoryFileImpl();
        OperationResult duplicate = apply(restarted);

        assertTrue(first.applied());
        assertFalse(duplicate.applied());
        assertEquals(first.operation().getId(), duplicate.operation().getId());
        assertEquals(1, restarted.findHistory("Player", 10, 0).size());
        assertEquals(first.operation().getAfter().getValidUntil(), restarted.findByName("Player").orElseThrow().getValidUntil());
        assertThrows(IllegalArgumentException.class, () -> restarted.applyOperation("Player", metadata,
                "different request", (player, history) -> { throw new AssertionError(); }));
    }

    @Test
    void failedOperation_doesNotChangePlayerOrHistory() {
        apply(repo);
        LocalDateTime validUntil = repo.findByName("Player").orElseThrow().getValidUntil();

        assertThrows(IllegalStateException.class, () -> repo.applyOperation("Player", OperationMetadata.system("test"),
                "failed", (player, history) -> {
                    player.orElseThrow().setValidUntil(validUntil.plusDays(1));
                    throw new IllegalStateException("Failed");
                }));

        assertEquals(validUntil, repo.findByName("Player").orElseThrow().getValidUntil());
        assertEquals(1, repo.findHistory("Player", 10, 0).size());
    }

    @Test
    void malformedFile_isNotReplacedWithEmptyState() throws Exception {
        Path path = Path.of(JoupenProperties.playersFilepath);
        Files.writeString(path, "broken json");

        assertThrows(IllegalStateException.class, () -> apply(repo));
        assertEquals("broken json", Files.readString(path));
    }

    @Test
    void simultaneousDuplicateRequests_applyOnlyOnce() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<OperationResult> request = () -> {
            start.await();
            return apply(new PlayerRepositoryFileImpl());
        };
        try {
            var first = executor.submit(request);
            var second = executor.submit(request);
            start.countDown();
            assertNotEquals(first.get(10, TimeUnit.SECONDS).applied(), second.get(10, TimeUnit.SECONDS).applied());
            assertEquals(1, repo.findHistory("Player", 10, 0).size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void legacyUpdateAndDeletePreserveHistoryAndExternalIds() {
        var original = apply(repo).operation();
        PlayerEntity player = repo.findByName("Player").orElseThrow();
        player.setApproved(true);
        repo.updateByName(player, "Player");
        assertEquals(1, repo.findHistory("Player", 10, 0).size());
        assertFalse(repo.findHistory("Player", 10, 0).get(0).getAfter().getApproved());
        repo.delete(player.getUuid());
        assertTrue(repo.findByName("Player").isEmpty());
        assertEquals(original.getId(), repo.findHistory("Player", 10, 0).get(0).getId());
        assertFalse(apply(repo).applied());
        assertTrue(repo.findByName("Player").isEmpty());
    }

    private OperationResult apply(PlayerRepositoryFileImpl repository) {
        return repository.applyOperation("Player", metadata, "Player:gift:86400", (current, history) -> {
            LocalDateTime now = LocalDateTime.now().withNano(0);
            PlayerEntity player = new PlayerEntity("Player", false, UUID.randomUUID(), now.plusDays(1), now);
            return PlayerOperation.builder().type(OperationType.GIFT).durationSeconds(86400)
                    .occurredAt(now).before(current.orElse(null)).after(player).build();
        });
    }
}
