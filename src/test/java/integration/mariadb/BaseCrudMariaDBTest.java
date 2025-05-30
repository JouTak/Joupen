package integration.mariadb;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.joupen.domain.PlayerEntity;
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

    protected static SessionFactory sessionFactory;

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

        // Настройка Hibernate
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.url", mariaDB.getJdbcUrl());
        configuration.setProperty("hibernate.connection.username", mariaDB.getUsername());
        configuration.setProperty("hibernate.connection.password", mariaDB.getPassword());
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "validate");
        configuration.setProperty("hibernate.show_sql", "true");
        configuration.setProperty("hibernate.format_sql", "true");
        configuration.addAnnotatedClass(PlayerEntity.class);

        sessionFactory = configuration.buildSessionFactory();
    }

    @AfterAll
    static void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        mariaDB.stop();
    }

    @BeforeEach
    void clearDatabase() {
        try (var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            session.createNativeQuery("DELETE FROM players").executeUpdate();
            transaction.commit();
        }
    }
}