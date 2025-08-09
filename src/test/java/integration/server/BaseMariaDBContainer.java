package integration.server;

import org.testcontainers.containers.MariaDBContainer;

public class BaseMariaDBContainer {
    public static final MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:10.11")
            .withDatabaseName("mydb")
            .withUsername("testuser")
            .withPassword("testpass");

    public static void cleanUpDatabase() {
        try (var connection = mariaDB.createConnection("")) {
            var statement = connection.createStatement();
            statement.executeUpdate("DELETE FROM players");
        } catch (Exception e) {
            throw new RuntimeException("Failed to clean up database", e);
        }
    }
}