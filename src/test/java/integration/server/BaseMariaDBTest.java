package integration.server;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MariaDBContainer;

public abstract class BaseMariaDBTest {

    protected static MariaDBContainer<?> mariaDB;
    protected static HikariDataSource dataSource;
    protected static DSLContext dsl;

    @BeforeAll
    static void startMariaDB() throws Exception {
        mariaDB = new MariaDBContainer<>("mariadb:10.11")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
        mariaDB.start();

        Database db = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(mariaDB.createConnection("")));
        Liquibase liquibase = new Liquibase("db/changelog/master.yaml", new ClassLoaderResourceAccessor(), db);
        liquibase.update("");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(mariaDB.getJdbcUrl());
        config.setUsername(mariaDB.getUsername());
        config.setPassword(mariaDB.getPassword());
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        dataSource = new HikariDataSource(config);
        dsl = DSL.using(dataSource, SQLDialect.MARIADB);

        System.out.println("🟢 MariaDB started at " + mariaDB.getJdbcUrl());
    }

    @AfterAll
    static void stopMariaDB() {
        if (dataSource != null) dataSource.close();
        if (mariaDB != null) mariaDB.stop();
        System.out.println("🧹 MariaDB stopped.");
    }
}
