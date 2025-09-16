package integration.mariadb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.joupen.jooq.generated.default_schema.tables.Players;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class BaseCrudMariaDBTest {

    @Container
    protected static final MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:10.11")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    protected static DSLContext dslContext;
    protected static HikariDataSource dataSource;

    @BeforeAll
    static void setUp() throws Exception {
        // Запуск контейнера MariaDB
        mariaDB.start();

        // Применение миграций Liquibase
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(
                        new JdbcConnection(mariaDB.createConnection(""))
                );
        Liquibase liquibase = new Liquibase(
                "db/changelog/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(),
                database
        );
        liquibase.update("");

        // Настройка HikariCP и jOOQ
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(mariaDB.getJdbcUrl());
        config.setUsername(mariaDB.getUsername());
        config.setPassword(mariaDB.getPassword());
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
        dslContext = DSL.using(dataSource, SQLDialect.MARIADB);
    }

    @AfterAll
    static void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        mariaDB.stop();
    }

    @BeforeEach
    void clearDatabase() {
        dslContext.transaction(configuration -> {
            DSL.using(configuration)
                    .deleteFrom(Players.PLAYERS)
                    .execute();
        });
    }
}