package integration.server.docker;

import com.github.t9t.minecraftrconclient.RconClient;
import integration.server.BaseMariaDBContainer;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.time.Duration;

@Testcontainers
public abstract class BasePurpurIntegrationTest extends BaseMariaDBContainer {

    private static final Logger LOGGER =  LogManager.getLogger(BasePurpurIntegrationTest.class);
    private static final Network network = Network.newNetwork();
    protected static RconClient rconClient;

    private static class CustomLogConsumer extends ToStringConsumer {
        private final Logger logger;
        private final String prefix;

        public CustomLogConsumer(Logger logger, String prefix) {
            this.logger = logger;
            this.prefix = prefix;
        }

        @Override
        public void accept(OutputFrame outputFrame) {
            super.accept(outputFrame);
            String message = outputFrame.getUtf8String().trim();
            if (!message.isEmpty()) {
                logger.info("[{}] {}", prefix, message);
            }
        }
    }

    protected static final GenericContainer<?> purpurServer = new GenericContainer<>(DockerImageName.parse("itzg/minecraft-server:latest"))
            .withNetwork(network)
            .withExposedPorts(25565, 25575)
            .withEnv("EULA", "TRUE")
            .withEnv("TYPE", "PURPUR")
            .withEnv("VERSION", "1.20.2")
            .withEnv("MEMORY", "3G")
            .withEnv("ENABLE_COMMAND_BLOCK", "TRUE")
            .withEnv("ONLINE_MODE", "FALSE")
            .withEnv("ENFORCE_WHITELIST", "TRUE")
            .withEnv("RCON_ENABLED", "TRUE")
            .withEnv("RCON_PASSWORD", "testpassword")
            .withEnv("RCON_PORT", "25575")
            .waitingFor(Wait.forLogMessage(".*Done \\(.*\\)! For help, type.*", 1))
            .withStartupTimeout(Duration.ofSeconds(900))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Paths.get("target/LoginPluginForJoutak-1.0-SNAPSHOT.jar")),
                    "/data/plugins/LoginPluginForJoutak-1.0-SNAPSHOT.jar"
            )
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Paths.get("src/test/resources/joupen/config.yml").toAbsolutePath()),
                    "/data/plugins/LoginPluginForJoutak/config.yml"
            )
            .dependsOn(mariaDB)
            .withLogConsumer(new CustomLogConsumer(LOGGER, "PurpurServer"));


    @BeforeAll
    static void setUp() throws Exception {
        mariaDB.withNetwork(network)
                .withNetworkAliases("mariadb")
                .waitingFor(Wait.forLogMessage(".*ready for connections.*", 1));

        LOGGER.info("Starting MariaDB...");
        mariaDB.start();
        String mariaDbJdbcUrl = mariaDB.getJdbcUrl();
        LOGGER.info("MariaDB started with JDBC URL: {}", mariaDbJdbcUrl);

        LOGGER.info("Starting Liquibase update...");
        try {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(mariaDB.createConnection("")));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update("");
            LOGGER.info("Liquibase schema update completed successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to apply Liquibase updates", e);
            throw e;
        }

        try (var connection = mariaDB.createConnection("")) {
            var result = connection.createStatement().executeQuery("SHOW TABLES LIKE 'players'");
            if (!result.next()) {
                throw new IllegalStateException("Table 'players' not found after Liquibase update");
            }
            LOGGER.info("Table 'players' exists after Liquibase update");
        }

        LOGGER.info("Starting Purpur server...");
        try {
            purpurServer.start();
            LOGGER.info("Purpur server started successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to start Purpur server: {}", purpurServer.getLogs());
            throw e;
        }

        String rconHost = purpurServer.getHost();
        Integer rconPort = purpurServer.getMappedPort(25575);
        String rconPassword = "testpassword";
        rconClient = RconClient.open(rconHost, rconPort, rconPassword);
        LOGGER.info("RCON client connected to {}:{}", rconHost, rconPort);
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
    void cleanDatabase() {
        cleanUpDatabase();
    }

    protected void executeCommandAsAdmin(String command) {
        String response = rconClient.sendCommand("op TestAdmin");
        LOGGER.info("Op command response: {}", response);
        response = rconClient.sendCommand(command);
        LOGGER.info("Command response: {}", response);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void simulatePlayerLogin(String playerName) {
        String response = rconClient.sendCommand("whitelist add " + playerName);
        LOGGER.info("Whitelist add response: {}", response);
    }
}