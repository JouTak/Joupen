package integration.server.bukkitTest;

import com.github.t9t.minecraftrconclient.RconClient;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import integration.server.PurpurConfig;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * Базовый класс для интеграционных тестов с Purpur в Docker.
 * <p>
 * Опционально поднимает MariaDB (если {@code config.useSql == true}),
 * прогоняет Liquibase миграции, и предоставляет RCON-доступ + DSLContext.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BasePurpurTest {

    protected static final String RCON_PASSWORD = "testpassword";
    protected static final int RCON_PORT = 25575;
    protected static final int MC_PORT = 25565;

    protected static final String MARIADB_NETWORK_ALIAS = "mariadb";
    protected static final String MARIADB_DATABASE = "Joupen";
    protected static final String MARIADB_USER = "user";
    protected static final String MARIADB_PASSWORD = "user_password";

    protected GenericContainer<?> purpur;
    protected MariaDBContainer<?> mariaDB;
    protected Path tempPluginsDir;
    protected PurpurConfig config;
    protected Network network;

    /**
     * jOOQ DSLContext для прямой проверки данных в MariaDB из теста (только
     * SQL-mode)
     */
    protected DSLContext testDsl;
    protected HikariDataSource testDataSource;

    @BeforeAll
    void setupContainers() throws Exception {
        config = configurePurpur();
        network = Network.newNetwork();

        // ─── 1. MariaDB ───────────────────────────────────────────────
        if (config.useSql) {
            mariaDB = new MariaDBContainer<>("mariadb:10.11")
                    .withDatabaseName(MARIADB_DATABASE)
                    .withUsername(MARIADB_USER)
                    .withPassword(MARIADB_PASSWORD)
                    .withNetwork(network)
                    .withNetworkAliases(MARIADB_NETWORK_ALIAS);
            mariaDB.start();
            System.out.println("🟢 MariaDB started: " + mariaDB.getJdbcUrl());

            // Прогоняем Liquibase миграции
            runLiquibase();
            System.out.println("✅ Liquibase migrations applied");

            // Создаём DSLContext для проверки данных из тестов
            HikariConfig hc = new HikariConfig();
            hc.setJdbcUrl(mariaDB.getJdbcUrl());
            hc.setUsername(mariaDB.getUsername());
            hc.setPassword(mariaDB.getPassword());
            hc.setDriverClassName("org.mariadb.jdbc.Driver");
            testDataSource = new HikariDataSource(hc);
            testDsl = DSL.using(testDataSource, SQLDialect.MARIADB);
            System.out.println("✅ Test DSLContext ready");
        }

        // ─── 2. Plugins directory ─────────────────────────────────────
        tempPluginsDir = Files.createTempDirectory("purpur-plugins");
        System.out.println("🧱 Temp plugins dir: " + tempPluginsDir);

        // Joupen jar
        Path joupenJar = findJoupenJar(config.joupenJarPath);
        Files.copy(joupenJar, tempPluginsDir.resolve("JoupenPlugin.jar"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✅ Copied JoupenPlugin.jar (" + Files.size(joupenJar) + " bytes)");

        // Plugin config
        Path pluginConfigDir = tempPluginsDir.resolve("JoupenPlugin");
        Files.createDirectories(pluginConfigDir);
        Files.writeString(pluginConfigDir.resolve("config.yml"), buildConfigYml());
        System.out.println("📋 Created config.yml for Joupen");

        // Начальный players.json (для file-mode)
        if (!config.useSql) {
            Files.writeString(pluginConfigDir.resolve("player.json"), "[]");
            System.out.println("📋 Created empty player.json");
        }

        // Custom configs
        if (config.customConfigDir != null && Files.exists(config.customConfigDir)) {
            copyDirectory(config.customConfigDir, pluginConfigDir);
            System.out.println("📦 Copied custom configs from " + config.customConfigDir);
        }

        // ─── 3. Purpur container ─────────────────────────────────────
        purpur = new GenericContainer<>("itzg/minecraft-server:java17")
                .withEnv("EULA", "TRUE")
                .withEnv("TYPE", "PURPUR")
                .withEnv("VERSION", "1.20.4")
                .withEnv("ONLINE_MODE", "false")
                .withEnv("ENABLE_RCON", "true")
                .withEnv("RCON_PASSWORD", RCON_PASSWORD)
                .withEnv("RCON_PORT", String.valueOf(RCON_PORT))
                .withEnv("MEMORY", "1G")
                .withEnv("JVM_OPTS", "-Xms512M -Xmx1G")
                .withExposedPorts(MC_PORT, RCON_PORT)
                .withCopyFileToContainer(MountableFile.forHostPath(tempPluginsDir), "/data/plugins")
                .withNetwork(network)
                .withStartupTimeout(Duration.ofMinutes(5))
                .waitingFor(Wait.forLogMessage(".*Done \\(.*\\)! For help, type \"help\".*", 1)
                        .withStartupTimeout(Duration.ofMinutes(5)))
                .withLogConsumer(this::printPurpurLogFrame);

        purpur.start();
        System.out.println("🟢 Purpur started on port " + purpur.getMappedPort(MC_PORT));
        TimeUnit.SECONDS.sleep(3);
    }

    @AfterAll
    void tearDownContainers() throws Exception {
        if (purpur != null) {
            try {
                System.out.println("===== FINAL PURPUR LOG DUMP =====");
                System.out.println(purpur.getLogs());
            } catch (Exception ignored) {
            } finally {
                purpur.stop();
            }
        }
        if (testDataSource != null)
            testDataSource.close();
        if (mariaDB != null) {
            mariaDB.stop();
            System.out.println("🧹 MariaDB stopped.");
        }
        if (tempPluginsDir != null) {
            Files.walk(tempPluginsDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
        if (network != null)
            network.close();
    }

    // ─── RCON ─────────────────────────────────────────────────────────

    protected String rcon(String command) {
        String host = purpur.getHost();
        int port = purpur.getMappedPort(RCON_PORT);
        try (RconClient client = RconClient.open(host, port, RCON_PASSWORD)) {
            String response = client.sendCommand(command);
            System.out.println("📡 RCON [" + command + "] → " + response);
            return response;
        }
    }

    protected boolean logsContain(String text) {
        return purpur.getLogs().contains(text);
    }

    /**
     * Читает файл из контейнера Purpur (например player.json).
     */
    protected String readFileFromContainer(String containerPath) {
        try {
            return purpur.copyFileFromContainer(containerPath, is -> new String(is.readAllBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Cannot read " + containerPath + " from container", e);
        }
    }

    // ─── Template ─────────────────────────────────────────────────────

    protected abstract PurpurConfig configurePurpur();

    // ─── Private ──────────────────────────────────────────────────────

    private void runLiquibase() throws Exception {
        Database db = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(mariaDB.createConnection("")));
        Liquibase liquibase = new Liquibase("db/changelog/master.yaml", new ClassLoaderResourceAccessor(), db);
        liquibase.update("");
    }

    private Path findJoupenJar(String configuredPath) throws IOException, InterruptedException {
        Path jar = Paths.get(configuredPath);
        if (Files.exists(jar))
            return jar;

        Path targetDir = Paths.get("target");
        Path foundJar = searchForJar(targetDir);

        if (foundJar != null) {
            return foundJar;
        }

        System.out.println("⚠️ Joupen jar not found. Attempting to build it via Maven...");
        buildJoupenJar();

        foundJar = searchForJar(targetDir);
        if (foundJar != null) {
            return foundJar;
        }

        throw new IllegalStateException("❌ Failed to build or find Joupen jar!");
    }

    private Path searchForJar(Path targetDir) throws IOException {
        if (!Files.exists(targetDir))
            return null;
        try (var stream = Files.list(targetDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith("Joupen-")
                            && p.getFileName().toString().endsWith(".jar")
                            && !p.getFileName().toString().contains("original"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private void buildJoupenJar() throws IOException, InterruptedException {
        String mvnCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
        ProcessBuilder pb = new ProcessBuilder(mvnCommand, "clean", "package", "-DskipTests");
        pb.directory(new java.io.File("."));
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[MAVEN BUILD] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Maven build failed with exit code: " + exitCode);
        }
        System.out.println("✅ Maven build completed successfully.");
    }

    private String buildConfigYml() {
        if (config.useSql) {
            String dbUrl = "jdbc:mariadb://" + MARIADB_NETWORK_ALIAS + ":3306/" + MARIADB_DATABASE;
            return """
                    plugin:
                      enabled: true
                      useSql: true
                      migrate: false
                    database:
                      url: %s
                      user: %s
                      password: %s
                      driver: org.mariadb.jdbc.Driver
                    """.formatted(dbUrl, MARIADB_USER, MARIADB_PASSWORD);
        } else {
            return """
                    plugin:
                      enabled: true
                      playersFile: player.json
                      useSql: false
                      migrate: false
                    """;
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void printPurpurLogFrame(OutputFrame frame) {
        if (frame == null)
            return;
        String text = frame.getUtf8String();
        if (text == null || text.isBlank())
            return;
        String prefix = frame.getType() == OutputFrame.OutputType.STDERR ? "[PURPUR ERR] " : "[PURPUR OUT] ";
        for (String line : text.split("\\R")) {
            if (!line.isBlank())
                System.out.println(prefix + line);
        }
    }
}
