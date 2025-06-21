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
import org.joupen.JoupenPlugin;
import org.joupen.database.DatabaseManager;
import org.joupen.database.TransactionManager;
import org.joupen.dto.PlayerDto;
import org.joupen.repository.PlayerRepository;
import org.joupen.repository.impl.PlayerRepositoryDbImpl;
import org.joupen.utils.JoupenProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Testcontainers
public abstract class BasePluginIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(BasePluginIntegrationTest.class);
    protected static ServerMock server;
    protected static JoupenPlugin plugin;
    protected static List<String> capturedMessages;
    protected static File testDir;
    protected static PlayerRepository playerRepository;

    @BeforeAll
    static void setUp() throws Exception {
        server = MockBukkit.mock();
        capturedMessages = new ArrayList<>();

        BaseMariaDBContainer.mariaDB.start();
        String mariaDbJdbcUrl = BaseMariaDBContainer.mariaDB.getJdbcUrl();
        System.out.println("MariaDB JDBC URL: " + mariaDbJdbcUrl);

        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(BaseMariaDBContainer.mariaDB.createConnection("")));
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

        Yaml yaml = new Yaml();
        String configContent = Files.readString(configPath);
        Map<String, Object> config = yaml.load(configContent);
        Map<String, Object> databaseConfig = (Map<String, Object>) config.get("database");
        databaseConfig.put("url", mariaDbJdbcUrl);

        File pluginDataFolder = new File(testDir, "plugins/JoupenPlugin");
        if (!pluginDataFolder.exists()) {
            pluginDataFolder.mkdirs();
        }

        File configFile = new File(pluginDataFolder, "config.yml");
        Files.writeString(configFile.toPath(), yaml.dump(config));
        Files.copy(playersJsonPath, pluginDataFolder.toPath().resolve("player.json"), StandardCopyOption.REPLACE_EXISTING);

        JoupenProperties.loadForTests(pluginDataFolder);

        plugin = MockBukkit.load(JoupenPlugin.class);
        server.getPluginManager().enablePlugin(plugin);

        if (!plugin.isEnabled()) {
            throw new IllegalStateException("Не удалось активировать плагин");
        }

        // Инициализация репозитория
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        TransactionManager transactionManager = new TransactionManager(databaseManager);
        playerRepository = new PlayerRepositoryDbImpl(databaseManager.getEntityManager(), transactionManager);

        server.setWhitelist(true);
        server.setWhitelistEnforced(true);

        System.out.println("Config.yml content: " + Files.readString(configFile.toPath()));
        System.out.println("Plugin dataFolder: " + pluginDataFolder.getAbsolutePath());
        System.out.println("Files in dataFolder: " + Arrays.toString(pluginDataFolder.list()));
    }

    @AfterAll
    static void tearDown() {
        if (plugin != null) {
            server.getPluginManager().disablePlugin(plugin);
        }
        MockBukkit.unmock();
        BaseMariaDBContainer.mariaDB.stop();
    }

    @BeforeEach
    public void cleanUpAndPopulateDatabase() {
        BaseMariaDBContainer.cleanUpDatabase();
        logger.info("База данных очищена");
        populateDatabase();
        verifyDatabasePopulation();
    }

    private void populateDatabase() {
        // Добавляем игрока Admin
        PlayerDto adminDto = new PlayerDto();
        adminDto.setName("Admin");
        adminDto.setUuid(UUID.randomUUID());
        adminDto.setPaid(true);
        adminDto.setLastProlongDate(LocalDateTime.now());
        adminDto.setValidUntil(LocalDateTime.now().plusDays(30));
        playerRepository.save(adminDto);
        logger.info("Сохранен игрок Admin");

        // Добавляем игрока TestPlayer из player.json
        PlayerDto testPlayerDto = new PlayerDto();
        testPlayerDto.setName("TestPlayer");
        testPlayerDto.setUuid(UUID.randomUUID());
        testPlayerDto.setPaid(true);
        testPlayerDto.setLastProlongDate(LocalDateTime.now());
        testPlayerDto.setValidUntil(LocalDateTime.now().plusDays(30));

        playerRepository.save(testPlayerDto);

        PlayerDto testPlayerExpiredDto = new PlayerDto();
       testPlayerExpiredDto.setName("ExpiredPlayer");
       testPlayerExpiredDto.setUuid(UUID.randomUUID());
       testPlayerExpiredDto.setPaid(true);
       testPlayerExpiredDto.setLastProlongDate(LocalDateTime.now().minusDays(60));
       testPlayerExpiredDto.setValidUntil(LocalDateTime.now().minusDays(30));
        logger.info("Сохранен игрок ExpiredPlayer");
        playerRepository.save(testPlayerExpiredDto);

        PlayerDto testPlayerValidDto = new PlayerDto();
        testPlayerValidDto.setName("ValidPlayer");
        testPlayerValidDto.setUuid(UUID.randomUUID());
        testPlayerValidDto.setPaid(true);
        testPlayerValidDto.setLastProlongDate(LocalDateTime.now());
        testPlayerValidDto.setValidUntil(LocalDateTime.now().plusDays(30));
        logger.info("Сохранен игрок ExpiredPlayer");
        playerRepository.save(testPlayerValidDto);
    }

    private void verifyDatabasePopulation() {
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        TransactionManager transactionManager = new TransactionManager(databaseManager);
        transactionManager.executeInTransaction(em -> {
            long adminCount = em.createQuery("SELECT COUNT(p) FROM PlayerEntity p WHERE p.name = :name", Long.class)
                    .setParameter("name", "Admin")
                    .getSingleResult();
            long testPlayerCount = em.createQuery("SELECT COUNT(p) FROM PlayerEntity p WHERE p.name = :name", Long.class)
                    .setParameter("name", "TestPlayer")
                    .getSingleResult();
            logger.info("Количество игроков Admin: {}", adminCount);
            logger.info("Количество игроков TestPlayer: {}", testPlayerCount);
            if (adminCount == 0 || testPlayerCount == 0) {
                throw new IllegalStateException("Не удалось заполнить базу: Admin или TestPlayer не найдены");
            }
        });
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