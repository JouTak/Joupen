package integration.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class BaseMariaDBContainer {
    private static final Logger LOGGER = LogManager.getLogger(BaseMariaDBContainer.class);

    public static final MariaDBContainer<?> mariaDB = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.11"))
            .withDatabaseName("mydb")
            .withUsername("user")
            .withPassword("user_password")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 1)
                    .withStartupTimeout(java.time.Duration.ofSeconds(60)));

    public static void cleanUpDatabase() {
        try (var connection = mariaDB.createConnection("")) {
            var statement = connection.createStatement();
            statement.executeUpdate("TRUNCATE TABLE players");
            LOGGER.info("Database table 'players' truncated");
        } catch (Exception e) {
            LOGGER.warn("Failed to clean up database: {}", e.getMessage());
            throw new RuntimeException("Не удалось очистить базу данных", e);
        }
    }
}