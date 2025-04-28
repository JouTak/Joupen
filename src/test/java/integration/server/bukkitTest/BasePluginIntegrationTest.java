package integration.server.bukkitTest;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import integration.server.BaseMariaDBContainer;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.joutak.loginpluginforjoutak.LoginPluginForJoutak;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@Testcontainers
public abstract class BasePluginIntegrationTest {

    protected static ServerMock server;
    protected static LoginPluginForJoutak plugin;
    protected static List<String> capturedMessages;
    protected static File testDir;
    protected static BaseMariaDBContainer baseMariaDBContainer = new BaseMariaDBContainer();

    @BeforeAll
    static void setUp() throws Exception {
        server = MockBukkit.mock();
        capturedMessages = new ArrayList<>();

        baseMariaDBContainer.mariaDB.start();
        String mariaDbJdbcUrl = baseMariaDBContainer.mariaDB.getJdbcUrl();
        System.out.println("MariaDB JDBC URL: " + mariaDbJdbcUrl);

        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(baseMariaDBContainer.mariaDB.createConnection("")));
        Liquibase liquibase = new Liquibase(
                "db/changelog/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(),
                database
        );
        liquibase.update("");

        Path tempDir = Files.createTempDirectory("mockbukkit-test");
        testDir = tempDir.toFile();

        Path configPath = Path.of("src/test/resources/joupen/config.yml").toAbsolutePath();
        Path playersJsonPath = Path.of("src/test/resources/joupen/player.json").toAbsolutePath();

        if (!Files.exists(configPath) || !Files.exists(playersJsonPath)) {
            throw new IllegalStateException("Требуемые тестовые файлы не найдены");
        }

        // Загружаем плагин
        plugin = MockBukkit.load(LoginPluginForJoutak.class);

        // Настраиваем папку данных и файлы конфигурации
        File pluginDataFolder = plugin.getDataFolder();
        if (!pluginDataFolder.exists()) {
            pluginDataFolder.mkdirs();
        }

        // Копируем config.yml и патчим URL
        String configContent = Files.readString(configPath);
        String updatedConfigContent = configContent.replace(
                "jdbc:mariadb://localhost:3306/mydb",
                mariaDbJdbcUrl
        );

        Files.writeString(pluginDataFolder.toPath().resolve("config.yml"), updatedConfigContent);
        Files.copy(playersJsonPath, pluginDataFolder.toPath().resolve("player.json"), StandardCopyOption.REPLACE_EXISTING);

        System.out.println("Config.yml content: " + Files.readString(pluginDataFolder.toPath().resolve("config.yml")));
        System.out.println("Plugin dataFolder: " + plugin.getDataFolder().getAbsolutePath());
        System.out.println("Files in dataFolder: " + Arrays.toString(pluginDataFolder.list()));

        // Активируем плагин
        server.getPluginManager().enablePlugin(plugin);

        if (!plugin.isEnabled()) {
            throw new IllegalStateException("Не удалось активировать плагин");
        }

        server.setWhitelist(true);
        server.setWhitelistEnforced(true);
    }

    @AfterAll
    static void tearDown() {
        if (plugin != null) {
            server.getPluginManager().disablePlugin(plugin);
        }
        MockBukkit.unmock();
        baseMariaDBContainer.mariaDB.stop();
    }

    @BeforeEach
    public void cleanUpDatabase() {
        baseMariaDBContainer.cleanUpDatabase();
    }

    protected PlayerMock createAdminPlayer() {
        PlayerMock admin = new MessageCapturingPlayerMock(server, "Admin", UUID.randomUUID());
        admin.setAddress(new InetSocketAddress("127.0.0.1", 25565));
        server.addPlayer(admin);
        admin.addAttachment(plugin, "joupen.admin", true);
        return admin;
    }

    protected PlayerMock createPlayer(String name) {
        PlayerMock player = new MessageCapturingPlayerMock(server, name, UUID.randomUUID());
        player.setAddress(new InetSocketAddress("127.0.0.1", 25565));
        server.addPlayer(player);
        return player;
    }

    protected static class MessageCapturingPlayerMock extends PlayerMock {
        public MessageCapturingPlayerMock(ServerMock server, String name, UUID uuid) {
            super(server, name, uuid);
        }

        @Override
        public void sendMessage(String message) {
            capturedMessages.add(message);
            super.sendMessage(message);
        }

        @Override
        public void sendMessage(String... messages) {
            for (String message : messages) {
                capturedMessages.add(message);
            }
            super.sendMessage(messages);
        }
    }
}