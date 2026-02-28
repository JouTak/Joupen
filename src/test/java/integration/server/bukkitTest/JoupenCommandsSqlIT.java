package integration.server.bukkitTest;

import integration.server.PurpurConfig;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест Joupen в SQL-mode (с MariaDB).
 * <p>
 * Поднимает MariaDB, прогоняет Liquibase миграции, стартует Purpur с плагином
 * и после каждой команды проверяет данные напрямую в БД.
 * <p>
 * Требует пересборки JAR: {@code mvn clean package -DskipTests}
 * (shade-plugin теперь сохраняет MariaDB-драйвер).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JoupenCommandsSqlIT extends BasePurpurTest {

    @Override
    protected PurpurConfig configurePurpur() {
        PurpurConfig cfg = new PurpurConfig();
        cfg.useSql = true;
        return cfg;
    }

    // ─── 1. Загрузка плагина ──────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[SQL] Plugin should load successfully")
    void pluginShouldLoad() {
        assertTrue(logsContain("JoupenPlugin enabled successfully!"),
                "Плагин Joupen должен быть загружен. Последние логи:\n" + purpur.getLogs().substring(
                        Math.max(0, purpur.getLogs().length() - 2000)));
    }

    // ─── 2. Таблица players должна существовать ──────────────────────

    @Test
    @Order(2)
    @DisplayName("[SQL] players table should exist after Liquibase migration")
    void playerTableShouldExist() {
        assertNotNull(testDsl, "testDsl не должен быть null (useSql=true)");

        int count = testDsl.fetchCount(table("players"));
        System.out.println("📊 players table count: " + count);
        // Таблица существует если fetchCount не бросил исключение
        assertTrue(count >= 0, "Таблица players должна существовать после Liquibase");
    }

    // ─── 3. /joupen help ──────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("[SQL] /joupen help — should return subcommands")
    void helpCommand() {
        String response = rcon("joupen help");
        assertNotNull(response);
        assertFalse(response.isBlank(), "Help не должен быть пустым");
        assertAll(
                () -> assertTrue(response.contains("prolong"), "Нет 'prolong' в: " + response),
                () -> assertTrue(response.contains("gift"), "Нет 'gift' в: " + response),
                () -> assertTrue(response.contains("info"), "Нет 'info' в: " + response));
    }

    // ─── 4. /joupen prolong + DB verification ─────────────────────────

    @Test
    @Order(4)
    @DisplayName("[SQL] /joupen prolong TestPlayer 30d — should insert into DB")
    void prolongAndVerifyDb() throws InterruptedException {
        String response = rcon("joupen prolong TestPlayer 30d");
        assertNotNull(response);
        assertFalse(response.contains("Unknown subcommand"), "Получено: " + response);

        // Даём серверу время записать в БД
        Thread.sleep(2000);

        // Проверяем напрямую в MariaDB
        Result<Record> result = testDsl.select()
                .from(table("players"))
                .where(field("name").eq("TestPlayer"))
                .fetch();

        System.out.println("📊 DB result after prolong: " + result);

        assertFalse(result.isEmpty(), "Игрок 'TestPlayer' должен быть в таблице players");

        Record player = result.get(0);
        assertEquals("TestPlayer", player.get(field("name", String.class)),
                "Имя игрока должно совпадать");

        java.sql.Timestamp validUntilTs = player.get(field("valid_until", java.sql.Timestamp.class));
        assertNotNull(validUntilTs, "valid_until не должен быть null");
        LocalDateTime validUntil = validUntilTs.toLocalDateTime();
        assertTrue(validUntil.isAfter(LocalDateTime.now()),
                "valid_until должен быть в будущем. Значение: " + validUntil);
    }

    // ─── 5. /joupen info from DB ──────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("[SQL] /joupen info TestPlayer — should return data from DB")
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
    @DisplayName("[SQL] /joupen unknowncmd — should return error")
    void unknownSubcommand() {
        String response = rcon("joupen nonexistentcommand");
        assertNotNull(response);
        assertTrue(response.contains("Unknown subcommand") || response.contains("help"),
                "Должна быть ошибка. Получено: " + response);
    }

    // ─── 7. /joupen gift + DB verification ────────────────────────────

    @Test
    @Order(7)
    @DisplayName("[SQL] /joupen gift GiftPlayer 10d — should insert into DB")
    void giftAndVerifyDb() throws InterruptedException {
        String response = rcon("joupen gift GiftPlayer 10d");
        assertNotNull(response);
        assertFalse(response.contains("Unknown subcommand"), "Получено: " + response);

        Thread.sleep(2000);

        Result<Record> result = testDsl.select()
                .from(table("players"))
                .where(field("name").eq("GiftPlayer"))
                .fetch();

        System.out.println("📊 DB result after gift: " + result);

        assertFalse(result.isEmpty(), "Игрок 'GiftPlayer' должен быть в таблице players");

        Record player = result.get(0);
        assertEquals("GiftPlayer", player.get(field("name", String.class)));

        java.sql.Timestamp validUntilTs = player.get(field("valid_until", java.sql.Timestamp.class));
        assertNotNull(validUntilTs, "valid_until не должен быть null");
        LocalDateTime validUntil = validUntilTs.toLocalDateTime();
        assertTrue(validUntil.isAfter(LocalDateTime.now()),
                "valid_until должен быть в будущем. Значение: " + validUntil);
    }

    // ─── 8. /joupen info для gift-игрока ──────────────────────────────

    @Test
    @Order(8)
    @DisplayName("[SQL] /joupen info GiftPlayer — should return data")
    void infoAfterGift() {
        String response = rcon("joupen info GiftPlayer");
        assertNotNull(response);
        assertTrue(response.contains("GiftPlayer"),
                "Info должен содержать 'GiftPlayer'. Получено: " + response);
    }

    // ─── 9. Проверка количества записей в таблице ─────────────────────

    @Test
    @Order(9)
    @DisplayName("[SQL] DB should contain exactly 2 players after all commands")
    void dbShouldContainTwoPlayers() {
        int count = testDsl.fetchCount(table("players"));
        System.out.println("📊 Total players in DB: " + count);
        assertEquals(2, count, "В таблице должно быть ровно 2 игрока (TestPlayer + GiftPlayer)");
    }
}
