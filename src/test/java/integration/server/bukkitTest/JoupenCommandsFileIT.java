package integration.server.bukkitTest;

import integration.server.PurpurConfig;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест Joupen в file-mode (без MariaDB).
 * <p>
 * После команд проверяет, что данные реально записались в player.json внутри
 * контейнера.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JoupenCommandsFileIT extends BasePurpurTest {

    @Override
    protected PurpurConfig configurePurpur() {
        PurpurConfig cfg = new PurpurConfig();
        cfg.useSql = false;
        return cfg;
    }

    // ─── 1. Загрузка плагина ──────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[FILE] Plugin should load successfully")
    void pluginShouldLoad() {
        assertTrue(logsContain("JoupenPlugin enabled successfully!"),
                "Плагин Joupen должен быть загружен. Логи:\n" + purpur.getLogs().substring(
                        Math.max(0, purpur.getLogs().length() - 2000)));
    }

    // ─── 2. /joupen help ──────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("[FILE] /joupen help — should return subcommands")
    void helpCommand() {
        String response = rcon("joupen help");
        assertNotNull(response);
        assertFalse(response.isBlank(), "Help не должен быть пустым");
        assertAll(
                () -> assertTrue(response.contains("prolong"), "Нет 'prolong' в: " + response),
                () -> assertTrue(response.contains("gift"), "Нет 'gift' в: " + response),
                () -> assertTrue(response.contains("info"), "Нет 'info' в: " + response));
    }

    // ─── 3. /joupen (no args) ─────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("[FILE] /joupen — no args = help")
    void noArgsIsHelp() {
        String response = rcon("joupen");
        assertNotNull(response);
        assertTrue(response.contains("prolong") || response.contains("Joupen"),
                "Без аргументов должен быть help. Получено: " + response);
    }

    // ─── 4. /joupen prolong + data verification ───────────────────────

    @Test
    @Order(4)
    @DisplayName("[FILE] /joupen prolong TestPlayer 30d — should create player in JSON")
    void prolongAndVerifyFile() throws InterruptedException {
        String response = rcon("joupen prolong TestPlayer 30d");
        assertNotNull(response);
        assertFalse(response.contains("Unknown subcommand"), "Получено: " + response);

        // Даём серверу время сохранить файл
        Thread.sleep(2000);

        // Читаем player.json из контейнера
        String json = readFileFromContainer("/data/plugins/JoupenPlugin/player.json");
        System.out.println("📄 player.json after prolong: " + json);

        assertTrue(json.contains("TestPlayer"),
                "player.json должен содержать 'TestPlayer'. Содержимое: " + json);
    }

    // ─── 5. /joupen info ──────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("[FILE] /joupen info TestPlayer — should return player data")
    void infoAfterProlong() {
        String response = rcon("joupen info TestPlayer");
        assertNotNull(response);
        assertFalse(response.isBlank(), "Info не должен быть пустым");
        assertTrue(response.contains("TestPlayer"),
                "Info должен содержать 'TestPlayer'. Получено: " + response);
    }

    // ─── 6. Unknown subcommand ────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("[FILE] /joupen unknowncmd — should return error")
    void unknownSubcommand() {
        String response = rcon("joupen nonexistentcommand");
        assertNotNull(response);
        assertTrue(response.contains("Unknown subcommand") || response.contains("help"),
                "Должна быть ошибка. Получено: " + response);
    }

    // ─── 7. /joupen prolong without args ──────────────────────────────

    @Test
    @Order(7)
    @DisplayName("[FILE] /joupen prolong — no args = usage")
    void prolongNoArgs() {
        String response = rcon("joupen prolong");
        assertNotNull(response);
        assertTrue(response.contains("Usage") || response.contains("Expected") || response.contains("prolong"),
                "Должен быть usage. Получено: " + response);
    }

    // ─── 8. /joupen gift + data verification ──────────────────────────

    @Test
    @Order(8)
    @DisplayName("[FILE] /joupen gift GiftPlayer 10d — should create player in JSON")
    void giftAndVerifyFile() throws InterruptedException {
        String response = rcon("joupen gift GiftPlayer 10d");
        assertNotNull(response);
        assertFalse(response.contains("Unknown subcommand"), "Получено: " + response);

        Thread.sleep(2000);

        String json = readFileFromContainer("/data/plugins/JoupenPlugin/player.json");
        System.out.println("📄 player.json after gift: " + json);

        assertTrue(json.contains("GiftPlayer"),
                "player.json должен содержать 'GiftPlayer'. Содержимое: " + json);
    }

    // ─── 9. /joupen info для gift-игрока ──────────────────────────────

    @Test
    @Order(9)
    @DisplayName("[FILE] /joupen info GiftPlayer — should return data")
    void infoAfterGift() {
        String response = rcon("joupen info GiftPlayer");
        assertNotNull(response);
        assertTrue(response.contains("GiftPlayer"),
                "Info должен содержать 'GiftPlayer'. Получено: " + response);
    }
}
