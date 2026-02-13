package integration.mariadb;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LiquibaseBooleanCompatibilityIT {

    @Test
    void shouldApplyChangelogOnPostgresWithNativeBoolean() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass")) {
            postgres.start();

            assertDoesNotThrow(() -> applyChangelog(postgres.createConnection("")));
        }
    }

    @Test
    void shouldApplyChangelogOnMariaDbWhereBooleanIsAlias() {
        try (MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:10.11")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass")) {
            maria.start();

            assertDoesNotThrow(() -> applyChangelog(maria.createConnection("")));
        }
    }

    private static void applyChangelog(java.sql.Connection connection) throws Exception {
        Database db = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase("db/changelog/master.yaml", new ClassLoaderResourceAccessor(), db);
        liquibase.update("");
    }
}
