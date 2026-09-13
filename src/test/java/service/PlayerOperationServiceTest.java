package service;

import org.joupen.domain.OperationException;
import org.joupen.domain.OperationMetadata;
import org.joupen.domain.OperationType;
import org.joupen.domain.PlayerEntity;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.service.PlayerOperationService;
import org.joupen.utils.JoupenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PlayerOperationServiceTest {
    @TempDir
    Path directory;
    private PlayerRepositoryFileImpl repo;
    private PlayerOperationService service;
    private final LocalDateTime now = LocalDateTime.of(2026, 8, 1, 12, 0);
    private final OperationMetadata metadata = new OperationMetadata("Correction", "command", "Admin", null);

    @BeforeEach
    void setUp() {
        JoupenProperties.playersFilepath = directory.resolve("player.json").toString();
        repo = new PlayerRepositoryFileImpl();
        service = at("2026-08-01T12:00:00Z");
    }

    @Test
    void undoSubtractsOriginalDurationWithoutReplayingLaterPurchases() {
        var first = service.grant("Player", Duration.ofDays(30), OperationType.PURCHASE, metadata);
        at("2026-08-10T12:00:00Z").grant("Player", Duration.ofDays(7), OperationType.PURCHASE, metadata);
        var undo = at("2026-08-12T12:00:00Z").undo("Player", first.operation().getId(), metadata);

        assertEquals(LocalDateTime.of(2026, 8, 8, 12, 0), undo.operation().getAfter().getValidUntil());
        assertEquals(-Duration.ofDays(30).getSeconds(), undo.operation().getDurationSeconds());
        assertEquals(first.operation().getId(), undo.operation().getReversedOperationId());
        assertEquals(3, service.history("Player", 1).size());
        assertEquals(now.plusDays(30), service.history("Player", 1).get(2).getAfter().getValidUntil());
    }

    @Test
    void adjustmentAndItsUndoAreOppositeChanges() {
        service.grant("Player", Duration.ofDays(10), OperationType.GIFT, metadata);
        var adjustment = service.adjust("Player", Duration.ofDays(-3), metadata);
        assertEquals(now.plusDays(7), adjustment.operation().getAfter().getValidUntil());
        assertEquals(now.plusDays(10), service.undo("Player", adjustment.operation().getId(), metadata).operation().getAfter().getValidUntil());
    }

    @Test
    void expiredPassCanBeReducedWithoutStartingANewPeriod() {
        var grant = service.grant("Player", Duration.ofDays(2), OperationType.GIFT, metadata);
        var undo = at("2026-08-10T12:00:00Z").undo("Player", grant.operation().getId(), metadata);
        assertEquals(now, undo.operation().getAfter().getValidUntil());
    }

    @Test
    void undoLastSkipsUndoEntriesAndAlreadyReversedOperations() {
        var first = service.grant("Player", Duration.ofDays(10), OperationType.PURCHASE, metadata);
        var second = service.grant("Player", Duration.ofDays(3), OperationType.COMPENSATION, metadata);
        assertEquals(second.operation().getId(), service.undo("Player", null, metadata).operation().getReversedOperationId());
        assertEquals(first.operation().getId(), service.undo("Player", null, metadata).operation().getReversedOperationId());
        assertThrows(OperationException.class, () -> service.undo("Player", null, metadata));
    }

    @Test
    void operationCanOnlyBeUndoneOnceAndOnlyForItsPlayer() {
        var grant = service.grant("Player", Duration.ofDays(3), OperationType.PURCHASE, metadata);
        assertThrows(OperationException.class, () -> service.undo("Other", grant.operation().getId(), metadata));
        service.undo("Player", grant.operation().getId(), metadata);
        assertThrows(OperationException.class, () -> service.undo("Player", grant.operation().getId(), metadata));
        assertEquals(2, service.history("Player", 1).size());
    }

    @Test
    void retryAfterUndoDoesNotApplyTheOriginalRequestAgain() {
        var external = new OperationMetadata("Payment", "shop", "Admin", "payment-123");
        var grant = service.grant("Player", Duration.ofDays(30), OperationType.PURCHASE, external);
        service.undo("Player", grant.operation().getId(), metadata);
        assertFalse(service.grant("Player", Duration.ofDays(30), OperationType.PURCHASE, external).applied());
        assertEquals(now, repo.findByName("Player").orElseThrow().getValidUntil());
        assertThrows(OperationException.class, () -> service.grant("Player", Duration.ofDays(31), OperationType.PURCHASE, external));
    }

    @Test
    void externalIdsAreScopedBySourceButNotPlayer() {
        var first = new OperationMetadata(null, "shop", null, "123");
        var second = new OperationMetadata(null, "event", null, "123");
        service.grant("Player", Duration.ofDays(1), OperationType.PURCHASE, first);
        assertTrue(service.grant("Player", Duration.ofDays(1), OperationType.GIFT, second).applied());
        assertThrows(OperationException.class, () -> service.grant("Other", Duration.ofDays(1), OperationType.PURCHASE, first));
    }

    @Test
    void approvalAndTemporaryAccessHaveIndependentUndo() {
        var approval = service.approval("Player", true, metadata);
        var temporary = service.temporaryAccess("Player", now, now.plusDays(1), metadata);
        service.undo("Player", approval.operation().getId(), metadata);
        PlayerEntity player = repo.findByName("Player").orElseThrow();
        assertFalse(player.getApproved());
        assertEquals(now.plusDays(1), player.getTemporaryAccessUntil());
        service.undo("Player", temporary.operation().getId(), metadata);
        assertNull(repo.findByName("Player").orElseThrow().getTemporaryAccessUntil());
    }

    @Test
    void firstJoinIsRecordedButNotOfferedAsLastUndo() {
        var grant = service.grant("Player", Duration.ofDays(30), OperationType.PURCHASE, metadata);
        var login = at("2026-08-05T12:00:00Z").bindOnLogin("Player", UUID.randomUUID(), false);
        assertEquals(Duration.ofDays(4).getSeconds(), login.operation().getDurationSeconds());
        assertEquals(grant.operation().getId(), service.undo("Player", null, metadata).operation().getReversedOperationId());
        assertThrows(OperationException.class, () -> service.undo("Player", login.operation().getId(), metadata));
    }

    @Test
    void invalidAmountsAndWindowsDoNotCreateHistory() {
        assertThrows(OperationException.class, () -> service.adjust("Player", Duration.ZERO, metadata));
        assertThrows(OperationException.class, () -> service.adjust("Player", Duration.ofDays(-1), metadata));
        assertThrows(OperationException.class, () -> service.grant("Player", Duration.ofDays(-1), OperationType.GIFT, metadata));
        assertThrows(OperationException.class, () -> service.temporaryAccess("Player", now, now, metadata));
        assertTrue(service.history("Player", 1).isEmpty());
    }

    @Test
    void simultaneousUndoCanOnlyReverseAnOperationOnce() throws Exception {
        var grant = service.grant("Player", Duration.ofDays(3), OperationType.PURCHASE, metadata);
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> undo = () -> {
            start.await();
            try {
                return service.undo("Player", grant.operation().getId(), metadata).applied();
            } catch (OperationException e) {
                assertEquals("operation-already-reversed", e.getMessage());
                return false;
            }
        };
        try {
            var first = executor.submit(undo);
            var second = executor.submit(undo);
            start.countDown();
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(now, repo.findByName("Player").orElseThrow().getValidUntil());
            assertEquals(2, service.history("Player", 1).size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void migrationRetryDoesNotOverwriteLaterOperations() {
        PlayerEntity legacy = new PlayerEntity("Player", true, UUID.randomUUID(), now.plusDays(2), now);
        OperationMetadata migration = new OperationMetadata("player.json", "migration", null, "snapshot-1");
        var first = service.migratePlayer(legacy, migration);
        service.adjust("Player", Duration.ofDays(3), metadata);
        assertFalse(service.migratePlayer(legacy, migration).applied());
        assertEquals(now.plusDays(5), repo.findByName("Player").orElseThrow().getValidUntil());
        assertEquals(2, service.history("Player", 1).size());
        assertThrows(OperationException.class, () -> service.undo("Player", first.operation().getId(), metadata));
    }

    @Test
    void importWithoutPreviousProlongDateRecordsTheRemainingTime() {
        PlayerEntity legacy = new PlayerEntity("Player", true, UUID.randomUUID(), now.plusDays(2), null);
        var result = service.importPlayer(legacy, metadata);
        assertEquals(Duration.ofDays(2).getSeconds(), result.operation().getDurationSeconds());
        assertEquals(now, service.undo("Player", result.operation().getId(), metadata).operation().getAfter().getValidUntil());
    }

    @Test
    void migrationMatchesExistingUuidAfterANameChange() {
        UUID uuid = UUID.randomUUID();
        service.grant("OldName", Duration.ofDays(1), OperationType.GIFT, metadata, uuid);
        PlayerEntity imported = new PlayerEntity("NewName", true, uuid, now.plusDays(2), now);
        OperationMetadata migration = new OperationMetadata("player.json", "migration", null, "snapshot-2");
        service.migratePlayer(imported, migration);
        assertEquals(1, repo.findAll().size());
        assertEquals(2, service.history("NewName", 1).size());
        assertFalse(service.migratePlayer(imported, migration).applied());
        assertEquals(now.plusDays(2), repo.findByUuid(uuid).orElseThrow().getValidUntil());
    }

    private PlayerOperationService at(String instant) {
        return new PlayerOperationService(repo, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }
}
