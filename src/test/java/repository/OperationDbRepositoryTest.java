package repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.joupen.database.DatabaseManager;
import org.joupen.database.TransactionManager;
import org.joupen.domain.OperationMetadata;
import org.joupen.domain.OperationResult;
import org.joupen.domain.OperationType;
import org.joupen.domain.PlayerEntity;
import org.joupen.domain.PlayerOperation;
import org.joupen.repository.impl.PlayerRepositoryDbImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationDbRepositoryTest {
    private HikariDataSource dataSource;
    private PlayerRepositoryDbImpl repo;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource = new HikariDataSource(config);
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()));
        try (Liquibase liquibase = new Liquibase("db/changelog/master.yaml", new ClassLoaderResourceAccessor(), database)) {
            liquibase.update("");
        }
        DatabaseManager manager = mock(DatabaseManager.class);
        when(manager.getDslContext()).thenReturn(DSL.using(dataSource, SQLDialect.H2));
        repo = new PlayerRepositoryDbImpl(new TransactionManager(manager));
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void duplicateRequest_returnsOriginalOperationWithoutApplyingAgain() {
        OperationResult first = apply("request-1");
        OperationResult duplicate = apply("request-1");

        assertTrue(first.applied());
        assertFalse(duplicate.applied());
        assertEquals(first.operation().getId(), duplicate.operation().getId());
        assertEquals(1, repo.findHistory("player", 10, 0).size());
        assertEquals(first.operation().getAfter().getValidUntil(), repo.findByName("Player").orElseThrow().getValidUntil());
    }

    @Test
    void failureAfterPlayerUpdate_rollsBackBothWrites() {
        apply("request-1");
        PlayerEntity before = repo.findByName("Player").orElseThrow();

        assertThrows(RuntimeException.class, () -> repo.applyOperation("Player", OperationMetadata.system("test"),
                "failure", (current, history) -> {
                    PlayerEntity after = current.orElseThrow();
                    after.setValidUntil(after.getValidUntil().plusDays(10));
                    return PlayerOperation.builder().after(after).build();
                }));

        assertEquals(before.getValidUntil(), repo.findByName("Player").orElseThrow().getValidUntil());
        assertEquals(1, repo.findHistory("Player", 10, 0).size());
    }

    @Test
    void concurrentRequests_doNotLoseUpdatesOrCreateDuplicatePlayers() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> { start.await(); return apply("request-1"); });
            var second = executor.submit(() -> { start.await(); return apply("request-2"); });
            start.countDown();
            assertTrue(first.get().applied());
            assertTrue(second.get().applied());
            assertEquals(1, repo.findAll().size());
            assertEquals(2, repo.findHistory("Player", 10, 0).size());
            assertEquals(LocalDateTime.of(2026, 8, 30, 12, 0), repo.findByName("Player").orElseThrow().getValidUntil());
        } finally {
            executor.shutdownNow();
        }
    }

    private OperationResult apply(String externalId) {
        return repo.applyOperation("Player", new OperationMetadata("Test", "test", "Admin", externalId),
                "Player:gift:86400", (current, history) -> {
                    LocalDateTime now = LocalDateTime.of(2026, 8, 28, 12, 0);
                    PlayerEntity before = current.map(PlayerOperation::snapshot).orElse(null);
                    PlayerEntity after = current.orElseGet(() -> new PlayerEntity("Player", false, UUID.randomUUID(), now, now));
                    after.setValidUntil(after.getValidUntil().plusDays(1));
                    return PlayerOperation.builder().type(OperationType.GIFT).durationSeconds(86400)
                            .occurredAt(now).before(before).after(after).build();
                });
    }
}
