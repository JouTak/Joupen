package integration.server.docker;

import com.github.t9t.minecraftrconclient.RconClient;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Testcontainers
public abstract class BasePurpurIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePurpurIntegrationTest.class);
    private static final Network network = Network.newNetwork();

    @Container
    protected static final MariaDBContainer<?> mariaDB = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.11"))
            .withDatabaseName("mydb")
            .withUsername("user")
            .withPassword("user_password")
            .withNetwork(network)
            .withNetworkAliases("mariadb")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 1))
            .withStartupTimeout(Duration.ofSeconds(60))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("MariaDB"));

    @Container
    protected static final GenericContainer<?> purpurServer;

    static {
        try {
            purpurServer = new GenericContainer<>(DockerImageName.parse("itzg/minecraft-server:latest"))
                    .withNetwork(network)
                    .withExposedPorts(25565, 25575)
                    .withEnv("EULA", "TRUE")
                    .withEnv("TYPE", "PURPUR")
                    .withEnv("VERSION", "1.20.2")
                    .withEnv("MEMORY", "3G") // Увеличено до 3 ГБ для надежности
                    .withEnv("ENABLE_COMMAND_BLOCK", "TRUE")
                    .withEnv("ONLINE_MODE", "FALSE")
                    .withEnv("ENFORCE_WHITELIST", "TRUE")
                    .withEnv("RCON_ENABLED", "TRUE")
                    .withEnv("RCON_PASSWORD", "testpassword")
                    .withEnv("RCON_PORT", "25575")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(Paths.get("target/LoginPluginForJoutak-1.0-SNAPSHOT.jar")),
                            "/data/plugins/LoginPluginForJoutak-1.0-SNAPSHOT.jar"
                    )
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(createTempConfigFile()),
                            "/data/plugins/LoginPluginForJoutak/config.yml"
                    )
                    .waitingFor(Wait.forLogMessage(".*Done \\(.*\\)! For help, type.*", 1))
                    .withStartupTimeout(Duration.ofSeconds(300)) // Увеличен таймаут до 5 минут
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("PurpurServer").withSeparateOutputStreams());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Purpur container", e);
        }
    }

    protected static RconClient rconClient;

    @BeforeAll
    static void setUp() throws Exception {
        // Запускаем MariaDB и проверяем готовность
        mariaDB.start();
        try (var connection = mariaDB.createConnection("")) {
            LOGGER.info("MariaDB is ready and accepting connections at {}", mariaDB.getJdbcUrl());
        } catch (Exception e) {
            LOGGER.error("MariaDB is not ready. Logs:\n{}", mariaDB.getLogs());
            throw new IllegalStateException("MariaDB is not ready", e);
        }

        String mariaDbJdbcUrl = "jdbc:mariadb://mariadb:3306/mydb";
        LOGGER.info("MariaDB JDBC URL for Purpur: {}", mariaDbJdbcUrl);

        // Инициализация базы данных с помощью Liquibase
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(mariaDB.createConnection("")));
        Liquibase liquibase = new Liquibase(
                "db/changelog/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(),
                database
        );
        liquibase.update("");
        LOGGER.info("Liquibase schema update completed");

        // Копирование player.json
        Path playerJsonPath = Paths.get("src/test/resources/joupen/player.json").toAbsolutePath();
        if (!Files.exists(playerJsonPath)) {
            throw new IllegalStateException("Файл player.json не найден: " + playerJsonPath);
        }
        purpurServer.copyFileToContainer(
                MountableFile.forHostPath(playerJsonPath),
                "/data/plugins/LoginPluginForJoutak/player.json"
        );
        LOGGER.info("Copied player.json to container");

        // Запуск Purpur-сервера с детальным логированием
        try {
            purpurServer.start();
            LOGGER.info("Purpur server started successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to start Purpur container. Container logs:\n{}", purpurServer.getLogs());
            throw new RuntimeException("Failed to start Purpur container", e);
        }

        // Подключение к RCON
        String rconHost = purpurServer.getHost();
        Integer rconPort = purpurServer.getMappedPort(25575);
        String rconPassword = "testpassword";
        try {
            rconClient = RconClient.open(rconHost, rconPort, rconPassword);
            LOGGER.info("RCON client connected to {}:{}", rconHost, rconPort);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize RCON client. Container logs:\n{}", purpurServer.getLogs());
            throw new RuntimeException("Cannot connect to RCON", e);
        }
    }

    @AfterAll
    static void tearDown() {
        if (rconClient != null) {
            rconClient.close();
            LOGGER.info("RCON client closed");
        }
        purpurServer.stop();
        LOGGER.info("Purpur server stopped");
        mariaDB.stop();
        LOGGER.info("MariaDB stopped");
    }

    @BeforeEach
    void cleanUpDatabase() {
        try (var connection = mariaDB.createConnection("")) {
            var statement = connection.createStatement();
            statement.executeUpdate("TRUNCATE TABLE players");
            LOGGER.info("Database table 'players' truncated");
        } catch (Exception e) {
            LOGGER.error("Failed to clean up database", e);
            throw new RuntimeException("Не удалось очистить базу данных", e);
        }
    }

    protected void executeCommandAsAdmin(String command) {
        String response = rconClient.sendCommand("op TestAdmin");
        LOGGER.info("Op command response: {}", response);
        response = rconClient.sendCommand(command);
        LOGGER.info("Command response: {}", response);
    }

    protected void simulatePlayerLogin(String playerName) {
        String response = rconClient.sendCommand("whitelist add " + playerName);
        LOGGER.info("Whitelist add response: {}", response);
    }

    private static Path createTempConfigFile() throws Exception {
        Path configPath = Paths.get("src/test/resources/joupen/config.yml").toAbsolutePath();
        if (!Files.exists(configPath)) {
            throw new IllegalStateException("Файл config.yml не найден: " + configPath);
        }
        String configContent = Files.readString(configPath);
        String updatedConfigContent = configContent.replace(
                "jdbc:mariadb://localhost:3306/mydb",
                "jdbc:mariadb://mariadb:3306/mydb"
        );
        // Добавляем driver, если его нет
        if (!updatedConfigContent.contains("driver:")) {
            updatedConfigContent += "\n  driver: org.mariadb.jdbc.Driver";
        }
        Path tempConfigPath = Files.createTempFile("config", ".yml");
        Files.writeString(tempConfigPath, updatedConfigContent);
        LOGGER.info("Created temporary config.yml with content:\n{}", updatedConfigContent);
        return tempConfigPath;
    }
}