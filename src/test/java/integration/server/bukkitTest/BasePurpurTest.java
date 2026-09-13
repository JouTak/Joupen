package integration.server.bukkitTest;

import com.github.t9t.minecraftrconclient.RconClient;
import integration.server.PurpurConfig;
import integration.server.util.PluginBuildUtils;
import integration.server.util.TestCacheUtils;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public abstract class BasePurpurTest {

    protected GenericContainer<?> purpur;
    protected MariaDBContainer<?> mariadb;
    protected Network network;
    protected Path tempPluginsDir;
    protected Path purpurCacheDir;
    protected PurpurConfig config;
    protected RconClient rconClient;
    protected StringBuilder purpurLogs = new StringBuilder();

    @BeforeAll
    void setupPurpur() throws Exception {
        config = configurePurpur();

        tempPluginsDir = Files.createTempDirectory("plugins");
        System.out.println("🧱 Plugins dir: " + tempPluginsDir);

        // 1️⃣ Joupen
        Path joupenJar = PluginBuildUtils.getOrBuildJoupenJar(config.joupenJarPath);
        System.out.println("🔍 Using Joupen jar at: " + joupenJar.toAbsolutePath());
        Files.copy(joupenJar, tempPluginsDir.resolve("JoupenPlugin.jar"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✅ Copied JoupenPlugin.jar to container");

        // 2️⃣ DiscordSRV (опционально)
        if (config.enableDiscordSrv) {
            TestCacheUtils.downloadDiscordSrv(config.discordVersion, tempPluginsDir);
        }

        // 3️⃣ MariaDB container (если SQL режим)
        if (config.useSql) {
            network = Network.newNetwork();
            mariadb = new MariaDBContainer<>("mariadb:10.11")
                    .withDatabaseName("Joupen")
                    .withUsername(config.mariaDbUser)
                    .withPassword(config.mariaDbPassword)
                    .withNetwork(network)
                    .withNetworkAliases("mariadb");
            mariadb.start();
            System.out.println("🗄️ MariaDB started at " + mariadb.getJdbcUrl());

            // Прогоняем Liquibase миграции из тестового кода
            // (liquibase-core имеет scope=provided, не попадает в fat JAR плагина)
            runLiquibaseMigrations();

            // Создаём config.yml для Joupen с SQL настройками
            // migrate: false — миграции уже прогнаны из тестового кода
            // Purpur подключается к MariaDB через общую Docker-сеть по алиасу "mariadb"
            Path pluginConfigDir = tempPluginsDir.resolve("JoupenPlugin");
            Files.createDirectories(pluginConfigDir);
            String configYml = String.format("""
                    plugin:
                      enabled: true
                      useSql: true
                      migrate: false
                    database:
                      url: jdbc:mariadb://mariadb:3306/Joupen
                      user: %s
                      password: %s
                      driver: org.mariadb.jdbc.Driver
                    """, config.mariaDbUser, config.mariaDbPassword);
            Files.writeString(pluginConfigDir.resolve("config.yml"), configYml);
            System.out.println("📄 Created SQL config.yml");
        } else if (config.customConfigDir != null && Files.exists(config.customConfigDir)) {
            // 3️⃣ Конфиги Joupen (если указаны) - в папку плагина JoupenPlugin
            Path pluginConfigDir = tempPluginsDir.resolve("JoupenPlugin");
            Files.createDirectories(pluginConfigDir);

            Files.walk(config.customConfigDir).forEach(src -> {
                try {
                    Path relativePath = config.customConfigDir.relativize(src);
                    Path dest = pluginConfigDir.resolve(relativePath);
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("📄 Copied config: " + dest);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("📦 Copied custom Joupen configs to JoupenPlugin/");
        } else {
            // Создаём дефолтный config.yml для файлового режима
            Path pluginConfigDir = tempPluginsDir.resolve("JoupenPlugin");
            Files.createDirectories(pluginConfigDir);
            String configYml = """
                    plugin:
                      enabled: true
                      useSql: false
                    """;
            Files.writeString(pluginConfigDir.resolve("config.yml"), configYml);
            System.out.println("📄 Created default FILE config.yml");
        }

        // 4️⃣ Purpur container
        this.purpurCacheDir = TestCacheUtils.preparePurpurCache(tempPluginsDir, getClass().getSimpleName());

        GenericContainer<?> purpurBuilder = new GenericContainer<>("itzg/minecraft-server:java17")
                .withEnv("EULA", "TRUE")
                .withEnv("TYPE", "PURPUR")
                .withEnv("VERSION", "1.20.4")
                .withEnv("ENABLE_RCON", "true")
                .withEnv("RCON_PASSWORD", "test")
                .withEnv("RCON_PORT", "25575")
                .withEnv("DEBUG", "true")
                .withExposedPorts(25575)
                .withFileSystemBind(purpurCacheDir.toString(), "/data",
                        org.testcontainers.containers.BindMode.READ_WRITE)
                .withStartupTimeout(Duration.ofMinutes(8))
                .withLogConsumer(this::printPurpurLogFrame);

        // Purpur и MariaDB должны быть в одной Docker-сети для связи по алиасу
        // "mariadb"
        if (network != null) {
            purpurBuilder = purpurBuilder.withNetwork(network);
        }

        purpur = purpurBuilder;

        // Ретрай запуска контейнера (скачивание Purpur/Mojang может упасть по таймауту)
        int maxRetries = 5;
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("🚀 Запуск Purpur контейнера (попытка " + attempt + "/" + maxRetries + ")...");
                purpur.start();
                lastException = null;
                break;
            } catch (Exception e) {
                lastException = e;
                System.err.println("❌ Попытка " + attempt + " не удалась: " + e.getMessage());
                try {
                    purpur.stop();
                } catch (Exception ignored) {
                }
                if (attempt < maxRetries) {
                    System.out.println("⏳ Ждём 10 сек перед повторной стадией...");
                    TimeUnit.SECONDS.sleep(10);
                    purpur = purpurBuilder;
                }
            }
        }
        if (lastException != null) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                    "Purpur контейнер не запустился после " + maxRetries + " попыток: " + lastException.getMessage());
        }

        // 5️⃣ RCON клиент
        String rconHost = purpur.getHost();
        int rconPort = purpur.getMappedPort(25575);
        System.out.println("🎮 RCON connecting to " + rconHost + ":" + rconPort);
        rconClient = RconClient.open(rconHost, rconPort, "test");
        System.out.println("✅ RCON authenticated");

        // Даём серверу время на полную загрузку
        TimeUnit.SECONDS.sleep(5);
    }

    private void printPurpurLogFrame(OutputFrame frame) {
        if (frame == null)
            return;

        String text = frame.getUtf8String();
        if (text == null || text.isBlank())
            return;

        purpurLogs.append(text);

        String prefix = frame.getType() == OutputFrame.OutputType.STDERR
                ? "[PURPUR ERR] "
                : "[PURPUR OUT] ";

        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                System.out.println(prefix + line);
            }
        }
    }

    @AfterAll
    void tearDownPurpur() throws Exception {
        if (rconClient != null) {
            try {
                rconClient.close();
            } catch (Exception ignored) {
            }
        }

        if (purpur != null) {
            try {
                System.out.println("===== FINAL PURPUR LOG DUMP =====");
                System.out.println(purpur.getLogs());
            } catch (Exception ignored) {
            } finally {
                purpur.stop();
            }
        }

        if (mariadb != null) {
            mariadb.stop();
        }

        if (network != null) {
            network.close();
        }

        TestCacheUtils.cleanTempDir(tempPluginsDir);
    }

    protected String rcon(String command) {
        try {
            System.out.println("📤 RCON: " + command);
            String response = rconClient.sendCommand(command);
            System.out.println("📥 Response: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("❌ RCON error: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    protected boolean logsContain(String text) {
        return purpurLogs.toString().contains(text);
    }

    protected abstract PurpurConfig configurePurpur();

    /**
     * Прогоняет Liquibase миграции из тестового кода.
     * Подключается к MariaDB через хостовый JDBC URL (не через Docker network).
     * Нужно потому что liquibase-core имеет scope=provided и не попадает в fat JAR
     * плагина.
     */
    private void runLiquibaseMigrations() {
        try {
            String jdbcUrl = mariadb.getJdbcUrl();
            String user = mariadb.getUsername();
            String password = mariadb.getPassword();

            System.out.println("🔄 Running Liquibase migrations against " + jdbcUrl);

            try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
                var database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                try (Liquibase liquibase = new Liquibase(
                        "db/changelog/master.yaml",
                        new ClassLoaderResourceAccessor(),
                        database)) {
                    liquibase.update("");
                }
            }

            System.out.println("✅ Liquibase migrations completed successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to run Liquibase migrations", e);
        }
    }
}
