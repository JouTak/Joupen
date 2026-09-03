package org.joupen.database;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class DatabaseSchemaManager {
    private final HikariDataSource dataSource;

    public DatabaseSchemaManager(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        uuid VARCHAR(36) NOT NULL,
                        last_prolong_date DATETIME NULL,
                        valid_until DATETIME NULL,
                        paid BOOLEAN NULL
                    )
                    """);
            boolean approvedExists = columnExists(connection, "players", "approved");
            if (!approvedExists) {
                statement.executeUpdate("ALTER TABLE players ADD COLUMN approved BOOLEAN NULL");
            }
            if (!approvedExists || columnNullable(connection, "players", "approved")) {
                statement.executeUpdate("UPDATE players SET approved = TRUE WHERE approved IS NULL");
                statement.executeUpdate("ALTER TABLE players MODIFY COLUMN approved BOOLEAN NOT NULL DEFAULT FALSE");
            }
            addColumn(connection, statement, "temporary_access_from", "DATETIME NULL");
            addColumn(connection, statement, "temporary_access_until", "DATETIME NULL");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS operation_lock (
                        id INT NOT NULL PRIMARY KEY
                    )
                    """);
            statement.executeUpdate("INSERT IGNORE INTO operation_lock (id) VALUES (1)");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_operations (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        player_id BIGINT NOT NULL,
                        player_name VARCHAR(16) NOT NULL,
                        type VARCHAR(32) NOT NULL,
                        duration_seconds BIGINT NOT NULL,
                        occurred_at DATETIME NOT NULL,
                        reason VARCHAR(1000) NULL,
                        source VARCHAR(64) NOT NULL,
                        initiator VARCHAR(128) NULL,
                        external_id VARCHAR(255) NULL,
                        external_key VARCHAR(64) NULL,
                        request VARCHAR(2000) NOT NULL,
                        reversed_operation_id BIGINT NULL,
                        state_before LONGTEXT NULL,
                        state_after LONGTEXT NOT NULL,
                        UNIQUE KEY uq_player_operations_external_key (external_key),
                        UNIQUE KEY uq_player_operations_reversal (reversed_operation_id),
                        KEY idx_player_operations_player (player_id, id)
                    )
                    """);
            addIndex(connection, statement, "uq_player_operations_external_key",
                    "CREATE UNIQUE INDEX uq_player_operations_external_key ON player_operations (external_key)");
            addIndex(connection, statement, "uq_player_operations_reversal",
                    "CREATE UNIQUE INDEX uq_player_operations_reversal ON player_operations (reversed_operation_id)");
            addIndex(connection, statement, "idx_player_operations_player",
                    "CREATE INDEX idx_player_operations_player ON player_operations (player_id, id)");
            log.info("Database schema is up to date");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update database schema", e);
        }
    }

    private void addColumn(Connection connection, Statement statement, String name, String definition) throws SQLException {
        if (!columnExists(connection, "players", name)) {
            statement.executeUpdate("ALTER TABLE players ADD COLUMN " + name + " " + definition);
        }
    }

    private void addIndex(Connection connection, Statement statement, String name, String sql) throws SQLException {
        String query = "SELECT COUNT(*) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'player_operations' AND INDEX_NAME = ?";
        try (PreparedStatement prepared = connection.prepareStatement(query)) {
            prepared.setString(1, name);
            try (ResultSet result = prepared.executeQuery()) {
                if (result.next() && result.getInt(1) == 0) statement.executeUpdate(sql);
            }
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        return columnValue(connection, table, column, "COUNT(*)").equals("1");
    }

    private boolean columnNullable(Connection connection, String table, String column) throws SQLException {
        return columnValue(connection, table, column, "IS_NULLABLE").equalsIgnoreCase("YES");
    }

    private String columnValue(Connection connection, String table, String column, String expression) throws SQLException {
        String sql = "SELECT " + expression + " FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return "0";
                return result.getString(1);
            }
        }
    }
}
