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
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;


@Testcontainers
public abstract class BasePluginIntegrationTest {

    protected static ServerMock server;
    protected static LoginPluginForJoutak plugin;
    protected static List<String> capturedMessages;
    protected static File testDir;
    protected static BaseMariaDBContainer baseMariaDBContainer = new BaseMariaDBContainer();

    @BeforeAll
    static void setUp() throws Exception {
        // Мокаем сервер и инициализируем список сообщений
        server = MockBukkit.mock();
        capturedMessages = new ArrayList<>();

        // Запускаем контейнер MariaDB и получаем JDBC URL
        baseMariaDBContainer.mariaDB.start();
        String mariaDbJdbcUrl = baseMariaDBContainer.mariaDB.getJdbcUrl();
        System.out.println("MariaDB JDBC URL: " + mariaDbJdbcUrl);

        // Настраиваем базу данных и применяем миграции через Liquibase
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(baseMariaDBContainer.mariaDB.createConnection("")));
        Liquibase liquibase = new Liquibase(
                "db/changelog/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(),
                database
        );
        liquibase.update("");

        // Создаем временную директорию для тестов
        Path tempDir = Files.createTempDirectory("mockbukkit-test");
        testDir = tempDir.toFile();

        // Проверяем наличие исходных файлов конфигурации
        Path configPath = Path.of("src/test/resources/joupen/config.yml").toAbsolutePath();
        Path playersJsonPath = Path.of("src/test/resources/joupen/player.json").toAbsolutePath();

        if (!Files.exists(configPath) || !Files.exists(playersJsonPath)) {
            throw new IllegalStateException("Требуемые тестовые файлы не найдены");
        }

        // Парсим YAML и обновляем URL базы данных
        Yaml yaml = new Yaml();
        String configContent = Files.readString(configPath);
        Map<String, Object> config = yaml.load(configContent);
        Map<String, Object> databaseConfig = (Map<String, Object>) config.get("database");
        databaseConfig.put("url", mariaDbJdbcUrl); // Устанавливаем правильный URL

        // Загружаем плагин
        plugin = MockBukkit.load(LoginPluginForJoutak.class);

        // Настраиваем папку данных плагина
        File pluginDataFolder = plugin.getDataFolder();
        if (!pluginDataFolder.exists()) {
            pluginDataFolder.mkdirs();
        }

        // Записываем обновленный config.yml и копируем player.json
        Files.writeString(pluginDataFolder.toPath().resolve("config.yml"), yaml.dump(config));
        Files.copy(playersJsonPath, pluginDataFolder.toPath().resolve("player.json"), StandardCopyOption.REPLACE_EXISTING);

        // Выводим отладочную информацию
        System.out.println("Config.yml content: " + Files.readString(pluginDataFolder.toPath().resolve("config.yml")));
        System.out.println("Plugin dataFolder: " + plugin.getDataFolder().getAbsolutePath());
        System.out.println("Files in dataFolder: " + Arrays.toString(pluginDataFolder.list()));

        // Активируем плагин
        server.getPluginManager().enablePlugin(plugin);

        // Проверяем, что плагин успешно активирован
        if (!plugin.isEnabled()) {
            throw new IllegalStateException("Не удалось активировать плагин");
        }

        // Настраиваем белый список
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