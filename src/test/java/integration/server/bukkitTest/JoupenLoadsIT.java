//package integration.server.bukkitTest;
//
//import integration.server.PurpurConfig;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//
//import java.nio.file.Path;
//import java.util.regex.Pattern;
//
/// **
// * Интеграционный тест: стартуем Purpur в Docker через Testcontainers и проверяем,
// * что Joupen плагин успешно загружается.
// */
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class JoupenLoadsIT extends BasePurpurTest {
//
//    @Override
//    protected PurpurConfig configurePurpur() {
//        PurpurConfig config = new PurpurConfig();
//
//        // Добавляем минимальную конфигурацию, чтобы плагин не отключался
//        try {
//            Path configDir = java.nio.file.Files.createTempDirectory("joupen-config");
//            Path configFile = configDir.resolve("config.yml");
//            java.nio.file.Files.writeString(configFile,
//                    "enabled: true\n" +
//                            "useSql: false\n" +
//                            "migrate: false\n"
//            );
//            config.customConfigDir = configDir;
//            System.out.println("📋 Created temp config at: " + configDir);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to create temp config", e);
//        }
//
//        return config;
//    }
//
//    @Test
//    void joupenShouldEnableSuccessfully() throws InterruptedException {
//        boolean loaded = false;
//        boolean serverStarted = false;
//
//        Pattern donePattern = Pattern.compile("Done \\([\\d.]+s\\)! For help, type \"(?:/)?help\"", Pattern.MULTILINE);
//
//        // Ждём до 8 минут (первый старт может быть долгим из-за скачивания/инициализации)
//        for (int i = 0; i < 480 && !loaded; i++) {
//            String logs = purpur.getLogs();
//
//            if (logs.contains("JoupenPlugin enabled successfully!")) {
//                loaded = true;
//            }
//
//            if (donePattern.matcher(logs).find()) {
//                serverStarted = true;
//            }
//
//            // Явные признаки ошибок плагина
//            if (logs.contains("UnsupportedClassVersionError")) {
//                Assertions.fail("Java mismatch (plugin compiled for newer Java than container runtime)\n" + logs);
//            }
//            if (logs.contains("Exception") && logs.contains("JoupenPlugin")) {
//                System.out.println("🚨 Detected Joupen-related exception in logs");
//            }
//
//            if (i % 10 == 0) {
//                System.out.println("⏳ Waiting " + i + "s... Server started: " + serverStarted + ", plugin loaded: " + loaded);
//            }
//
//            if (!loaded) {
//                Thread.sleep(1000);
//            }
//        }
//
//        System.out.println("Final state: loaded=" + loaded + ", serverStarted=" + serverStarted);
//
//        if (!loaded) {
//            System.out.println("===== PURPUR LOGS ON FAILURE =====");
//            System.out.println(purpur.getLogs());
//        }
//
//        Assertions.assertTrue(loaded, "Joupen должен успешно загрузиться в Purpur контейнере");
//    }
//}
