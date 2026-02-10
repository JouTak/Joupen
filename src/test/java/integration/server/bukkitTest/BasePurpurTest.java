//package integration.server.bukkitTest;
//
//import integration.server.PurpurConfig;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.containers.output.OutputFrame;
//import org.testcontainers.utility.MountableFile;
//
//import java.io.InputStream;
//import java.net.URL;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.time.Duration;
//import java.util.Comparator;
//
//public abstract class BasePurpurTest {
//
//    protected GenericContainer<?> purpur;
//    protected Path tempPluginsDir;
//    protected PurpurConfig config;
//
//    @BeforeEach
//    void setupPurpur() throws Exception {
//        config = configurePurpur();
//
//        tempPluginsDir = Files.createTempDirectory("plugins");
//        System.out.println("🧱 Plugins dir: " + tempPluginsDir);
//
//        // 1️⃣ Joupen
//        Path joupenJar = Paths.get(config.joupenJarPath);
//        System.out.println("🔍 Looking for Joupen jar at: " + joupenJar.toAbsolutePath());
//        if (!Files.exists(joupenJar)) {
//            throw new IllegalStateException("❌ Joupen plugin jar not found at " + joupenJar.toAbsolutePath());
//        }
//        System.out.println("📦 Joupen jar size: " + Files.size(joupenJar) + " bytes");
//        Files.copy(joupenJar, tempPluginsDir.resolve("JoupenPlugin.jar"), StandardCopyOption.REPLACE_EXISTING);
//        System.out.println("✅ Copied JoupenPlugin.jar to container");
//
//        // 2️⃣ DiscordSRV (опционально)
//        if (config.enableDiscordSrv) {
//            String fileName = "DiscordSRV-Build-" + config.discordVersion.substring(1) + ".jar";
//            String url = "https://github.com/DiscordSRV/DiscordSRV/releases/download/" + config.discordVersion + "/" + fileName;
//            System.out.println("📥 Downloading DiscordSRV " + config.discordVersion);
//            try (InputStream in = new URL(url).openStream()) {
//                Files.copy(in, tempPluginsDir.resolve("DiscordSRV.jar"), StandardCopyOption.REPLACE_EXISTING);
//            }
//        }
//
//        // 3️⃣ Конфиги Joupen (если указаны) - в папку плагина JoupenPlugin
//        if (config.customConfigDir != null && Files.exists(config.customConfigDir)) {
//            Path pluginConfigDir = tempPluginsDir.resolve("JoupenPlugin");
//            Files.createDirectories(pluginConfigDir);
//
//            Files.walk(config.customConfigDir).forEach(src -> {
//                try {
//                    Path relativePath = config.customConfigDir.relativize(src);
//                    Path dest = pluginConfigDir.resolve(relativePath);
//                    if (Files.isDirectory(src)) {
//                        Files.createDirectories(dest);
//                    } else {
//                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
//                        System.out.println("📄 Copied config: " + dest);
//                    }
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            System.out.println("📦 Copied custom Joupen configs to JoupenPlugin/");
//        }
//
//        // 4️⃣ Purpur container
//        purpur = new GenericContainer<>("itzg/minecraft-server:java17")
//                .withEnv("EULA", "TRUE")
//                .withEnv("TYPE", "PURPUR")
//                .withEnv("VERSION", "1.20.4")
//                .withEnv("ENABLE_RCON", "true")
//                .withEnv("RCON_PASSWORD", "test")
//                .withEnv("DEBUG", "true") // Больше диагностических логов от образа
//                .withCopyFileToContainer(MountableFile.forHostPath(tempPluginsDir), "/data/plugins")
//                .withStartupTimeout(Duration.ofMinutes(8))
//                .withLogConsumer(this::printPurpurLogFrame);
//
//        purpur.start();
//    }
//
//    private void printPurpurLogFrame(OutputFrame frame) {
//        if (frame == null) return;
//
//        String text = frame.getUtf8String();
//        if (text == null || text.isBlank()) return;
//
//        String prefix = frame.getType() == OutputFrame.OutputType.STDERR
//                ? "[PURPUR ERR] "
//                : "[PURPUR OUT] ";
//
//        for (String line : text.split("\\R")) {
//            if (!line.isBlank()) {
//                System.out.println(prefix + line);
//            }
//        }
//    }
//
//    @AfterEach
//    void tearDownPurpur() throws Exception {
//        if (purpur != null) {
//            try {
//                System.out.println("===== FINAL PURPUR LOG DUMP =====");
//                System.out.println(purpur.getLogs());
//            } catch (Exception ignored) {
//                // ignore
//            } finally {
//                purpur.stop();
//            }
//        }
//
//        if (tempPluginsDir != null) {
//            Files.walk(tempPluginsDir)
//                    .sorted(Comparator.reverseOrder())
//                    .map(Path::toFile)
//                    .forEach(java.io.File::delete);
//        }
//    }
//
//    /**
//     * Каждый тест определяет конфигурацию Purpur.
//     */
//    protected abstract PurpurConfig configurePurpur();
//}
