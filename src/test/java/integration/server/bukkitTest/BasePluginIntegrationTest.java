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
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.joupen.JoupenPlugin;
import org.joupen.database.DatabaseManager;
import org.joupen.database.TransactionManager;
import org.joupen.domain.PlayerEntity;
import org.joupen.jooq.generated.tables.Players;
import org.joupen.repository.PlayerRepository;
import org.joupen.repository.impl.PlayerRepositoryDbImpl;
import org.joupen.utils.JoupenProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.*;

import static integration.server.BaseMariaDBContainer.mariaDB;

@Testcontainers
@Slf4j
public abstract class BasePluginIntegrationTest {

    protected static ServerMock server;
    protected static JoupenPlugin plugin;
    protected static List<String> capturedMessages;
    protected static PlayerRepository playerRepository;

    @BeforeAll
    static void setUp() throws Exception {
        server = MockBukkit.mock();
        capturedMessages = new ArrayList<>();

        // стартуем MariaDB
        mariaDB.waitingFor(Wait.forLogMessage(".*ready for connections.*", 1));
        mariaDB.start();
        String mariaDbJdbcUrl = mariaDB.getJdbcUrl();

        // прогоняем миграции Liquibase
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(mariaDB.createConnection("")));
        Liquibase liquibase = new Liquibase(
                "db/changelog/master.yaml",
                new CompositeResourceAccessor(
                        new FileSystemResourceAccessor("target/classes/db/changelog"),
                        new ClassLoaderResourceAccessor()
                ),
                database
        );
        liquibase.update("");

        // Конфиг теперь задаём напрямую
        Map<String, Object> config = Map.of(
                "plugin", Map.of(
                        "enabled", true,
                        "useSql", true,
                        "playersFile", "player.json"
                ),
                "database", Map.of(
                        "url", mariaDbJdbcUrl,
                        "user", "root",
                        "password", mariaDB.getPassword(),
                        "driver", "org.mariadb.jdbc.Driver"
                )
        );
        JoupenProperties.initialize(config);

        // Загружаем плагин
        plugin = MockBukkit.load(JoupenPlugin.class);
        server.getPluginManager().enablePlugin(plugin);

        if (!plugin.isEnabled()) {
            throw new IllegalStateException("Не удалось активировать плагин");
        }

        // Репозиторий
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        TransactionManager transactionManager = new TransactionManager(databaseManager);
        playerRepository = new PlayerRepositoryDbImpl(transactionManager);

        server.setWhitelist(true);
        server.setWhitelistEnforced(true);
    }

    @AfterAll
    static void tearDown() {
        if (plugin != null) {
            server.getPluginManager().disablePlugin(plugin);
        }
        MockBukkit.unmock();
        mariaDB.stop();
    }

    @BeforeEach
    public void cleanUpAndPopulateDatabase() {
        BaseMariaDBContainer.cleanUpDatabase();
        populateDatabase();
    }

    private void populateDatabase() {
        savePlayer("Admin", 30);
        savePlayer("TestPlayer", 30);
        savePlayer("ExpiredPlayer", -30);
        savePlayer("ValidPlayer", 30);
    }

    private void savePlayer(String name, int daysOffset) {
        PlayerEntity p = new PlayerEntity();
        p.setName(name);
        p.setUuid(UUID.randomUUID());
        p.setPaid(true);
        p.setLastProlongDate(LocalDateTime.now());
        p.setValidUntil(LocalDateTime.now().plusDays(daysOffset));
        playerRepository.save(p);
        log.info("Сохранен игрок {}", name);
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
            capturedMessages.addAll(Arrays.asList(messages));
            super.sendMessage(messages);
        }
    }
}
