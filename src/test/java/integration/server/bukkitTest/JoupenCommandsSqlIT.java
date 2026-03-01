package integration.server.bukkitTest;

import integration.server.PurpurConfig;
import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест Joupen с MariaDB на настоящем Purpur-сервере в Docker.
 *
 * <p>
 * Что происходит:
 * <ol>
 * <li>Поднимается MariaDB + Purpur в Docker (через Testcontainers)</li>
 * <li>Joupen плагин загружается с SQL-конфигом, указывающим на MariaDB</li>
 * <li>Через RCON выполняются команды и проверяются ответы</li>
 * <li>Проверяется что данные сохраняются в MariaDB</li>
 * </ol>
 *
 * <p>
 * Требования: Docker запущен, проект собран ({@code mvn package -DskipTests}).
 *
 * <p>
 * Запуск:
 *
 * <pre>
 * mvn failsafe:integration-test failsafe:verify -Dit.test=JoupenCommandsSqlIT
 * </pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JoupenCommandsSqlIT extends BasePurpurTest {

    @Override
    protected PurpurConfig configurePurpur() {
        PurpurConfig cfg = new PurpurConfig();
        cfg.useSql = true;
        return cfg;
    }

    @Test
    @Order(1)
    @DisplayName("Joupen plugin should load successfully with SQL mode")
    void pluginShouldLoad() {
        assertTrue(logsContain("JoupenPlugin enabled successfully!"),
                "Плагин Joupen должен быть загружен. Проверьте логи сервера.");
    }

    @Test
    @Order(2)
    @DisplayName("/joupen help — should return help text with subcommands")
    void helpCommandShouldReturnSubcommands() {
        String response = rcon("joupen help");

        assertNotNull(response, "RCON ответ не должен быть null");
        assertFalse(response.isBlank(), "RCON ответ не должен быть пустым");

        assertAll("Help text должен содержать описание основных команд",
                () -> assertTrue(response.contains("prolong"),
                        "Ответ должен содержать 'prolong'. Получено: " + response),
                () -> assertTrue(response.contains("gift"),
                        "Ответ должен содержать 'gift'. Получено: " + response),
                () -> assertTrue(response.contains("info"),
                        "Ответ должен содержать 'info'. Получено: " + response));
    }

    @Test
    @Order(3)
    @DisplayName("/joupen prolong TestPlayer 30d — should prolong and save to DB")
    void prolongCommandShouldSaveToDatabase() throws SQLException {
        String response = rcon("joupen prolong TestPlayer 30d");

        assertNotNull(response, "RCON ответ не должен быть null");
        assertFalse(response.contains("Unknown subcommand"),
                "Сервер вернул 'Unknown subcommand'. Получено: " + response);

        try (Connection conn = DriverManager.getConnection(
                mariadb.getJdbcUrl(),
                mariadb.getUsername(),
                mariadb.getPassword())) {

            String sql = "SELECT name, valid_until, last_prolong_date FROM players WHERE name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "TestPlayer");
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next(), "Игрок TestPlayer должен быть в базе данных");

                    String name = rs.getString("name");
                    Timestamp validUntil = rs.getTimestamp("valid_until");
                    Timestamp lastProlong = rs.getTimestamp("last_prolong_date");

                    assertEquals("TestPlayer", name);
                    assertNotNull(validUntil, "valid_until не должен быть null");
                    assertNotNull(lastProlong, "last_prolong_date не должен быть null");

                    System.out.println("✅ DB: TestPlayer найден, valid_until=" + validUntil);
                }
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("/joupen info TestPlayer — should return info from DB")
    void infoCommandShouldReadFromDatabase() {
        String response = rcon("joupen info TestPlayer");

        assertNotNull(response, "RCON ответ не должен быть null");
        assertFalse(response.isBlank(), "RCON ответ не должен быть пустым");

        assertTrue(response.contains("TestPlayer"),
                "Info должен содержать имя игрока 'TestPlayer'. Получено: " + response);
    }

    @Test
    @Order(5)
    @DisplayName("/joupen prolong TestPlayer 10d — should update existing record")
    void prolongShouldUpdateExistingRecord() throws SQLException {
        Timestamp validUntilBefore;

        try (Connection conn = DriverManager.getConnection(
                mariadb.getJdbcUrl(),
                mariadb.getUsername(),
                mariadb.getPassword())) {

            String sql = "SELECT valid_until FROM players WHERE name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "TestPlayer");
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    validUntilBefore = rs.getTimestamp("valid_until");
                }
            }
        }

        String response = rcon("joupen prolong TestPlayer 10d");
        assertNotNull(response);

        try (Connection conn = DriverManager.getConnection(
                mariadb.getJdbcUrl(),
                mariadb.getUsername(),
                mariadb.getPassword())) {

            String sql = "SELECT valid_until FROM players WHERE name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "TestPlayer");
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    Timestamp validUntilAfter = rs.getTimestamp("valid_until");

                    assertTrue(validUntilAfter.after(validUntilBefore),
                            "valid_until должен быть обновлён. До: " + validUntilBefore + ", После: "
                                    + validUntilAfter);

                    System.out.println("✅ DB: valid_until обновлён с " + validUntilBefore + " на " + validUntilAfter);
                }
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("/joupen gift GiftPlayer 10d — should create new record in DB")
    void giftCommandShouldCreateNewRecord() throws SQLException {
        String response = rcon("joupen gift GiftPlayer 10d");

        assertNotNull(response, "RCON ответ не должен быть null");
        assertFalse(response.contains("Unknown subcommand"),
                "gift должна быть известной подкомандой. Получено: " + response);

        try (Connection conn = DriverManager.getConnection(
                mariadb.getJdbcUrl(),
                mariadb.getUsername(),
                mariadb.getPassword())) {

            String sql = "SELECT name, valid_until, paid FROM players WHERE name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "GiftPlayer");
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next(), "Игрок GiftPlayer должен быть в базе данных после gift");

                    String name = rs.getString("name");
                    Timestamp validUntil = rs.getTimestamp("valid_until");
                    boolean paid = rs.getBoolean("paid");

                    assertEquals("GiftPlayer", name);
                    assertNotNull(validUntil, "valid_until не должен быть null");
                    assertFalse(paid, "paid должен быть false для подарка");

                    System.out.println("✅ DB: GiftPlayer создан, paid=" + paid + ", valid_until=" + validUntil);
                }
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("/joupen info GiftPlayer — should show gift player from DB")
    void infoAfterGiftShouldWork() {
        String response = rcon("joupen info GiftPlayer");

        assertNotNull(response);
        assertTrue(response.contains("GiftPlayer"),
                "Info для GiftPlayer должен содержать имя. Получено: " + response);
    }

    @Test
    @Order(8)
    @DisplayName("DB should have exactly 2 players after all operations")
    void databaseShouldHaveTwoPlayers() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                mariadb.getJdbcUrl(),
                mariadb.getUsername(),
                mariadb.getPassword())) {

            String sql = "SELECT COUNT(*) as count FROM players";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                assertTrue(rs.next());
                int count = rs.getInt("count");

                assertEquals(2, count, "В базе должно быть ровно 2 игрока (TestPlayer и GiftPlayer)");
                System.out.println("✅ DB: Найдено " + count + " игроков");
            }
        }
    }

    @Test
    @Order(9)
    @DisplayName("DB schema should have all required tables")
    void databaseShouldHaveRequiredTables() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                mariadb.getJdbcUrl(),
                mariadb.getUsername(),
                mariadb.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "players", null)) {
                assertTrue(rs.next(), "Таблица 'players' должна существовать");
                System.out.println("✅ DB: Таблица 'players' существует");
            }

            try (ResultSet rs = metaData.getColumns(null, null, "players", null)) {
                boolean hasName = false;
                boolean hasUuid = false;
                boolean hasValidUntil = false;
                boolean hasPaid = false;

                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    if ("name".equalsIgnoreCase(columnName))
                        hasName = true;
                    if ("uuid".equalsIgnoreCase(columnName))
                        hasUuid = true;
                    if ("valid_until".equalsIgnoreCase(columnName))
                        hasValidUntil = true;
                    if ("paid".equalsIgnoreCase(columnName))
                        hasPaid = true;
                }

                assertTrue(hasName, "Колонка 'name' должна существовать");
                assertTrue(hasUuid, "Колонка 'uuid' должна существовать");
                assertTrue(hasValidUntil, "Колонка 'valid_until' должна существовать");
                assertTrue(hasPaid, "Колонка 'paid' должна существовать");

                System.out.println("✅ DB: Все обязательные колонки существуют");
            }
        }
    }
}
