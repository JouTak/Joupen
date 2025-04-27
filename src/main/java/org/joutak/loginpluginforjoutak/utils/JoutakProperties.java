package org.joutak.loginpluginforjoutak.utils;

import org.joutak.loginpluginforjoutak.LoginPluginForJoutak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class JoutakProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(JoutakProperties.class);
    private final LoginPluginForJoutak plugin;
    public static String playersFilepath;
    public static Boolean useSql;
    public static Boolean migrate;
    public static Boolean enabled = Boolean.TRUE;
    public static Map<String, Object> dbConfig;

    public JoutakProperties(LoginPluginForJoutak plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File configDir = plugin.getDataFolder();
        LOGGER.info("Plugin data folder: {}", configDir.getAbsolutePath());
        FileUtils.ensureDirectoryExists(configDir);
        if (!configDir.exists()) {
            LOGGER.error("Failed to create plugin data folder: {}", configDir.getAbsolutePath());
            throw new IllegalStateException("Plugin data folder does not exist: " + configDir.getAbsolutePath());
        }

        File configFile = new File(configDir, "config.yml");
        LOGGER.info("Checking config file: {}", configFile.getAbsolutePath());
        if (!configFile.exists()) {
            LOGGER.info("Config file does not exist, creating default config.yml");
            createDefaultConfig(configFile);
        } else {
            LOGGER.info("Config file already exists: {}", configFile.getAbsolutePath());
        }

        Map<String, Object> config = loadYaml(configFile);
        LOGGER.info("Loaded config: {}", config);

        Map<String, Object> pluginConfig = (Map<String, Object>) config.getOrDefault("plugin", Map.of());
        playersFilepath = new File(configDir, (String) pluginConfig.getOrDefault("saveFilepath", "player.json")).getPath();
        useSql = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("useSql", false)));
        migrate = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("migrate", false)));
        enabled = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("enabled", true)));
        LOGGER.info("Plugin config: playersFilepath={}, useSql={}, migrate={}, enabled={}",
                playersFilepath, useSql, migrate, enabled);

        if (useSql) {
            dbConfig = (Map<String, Object>) config.getOrDefault("database", Map.of());
            LOGGER.info("Database config: {}", dbConfig);
        } else {
            dbConfig = null;
            LOGGER.info("SQL disabled, dbConfig set to null");
        }
    }

    private Map<String, Object> loadYaml(File configFile) {
        try {
            if (!configFile.exists()) {
                LOGGER.error("Config file does not exist: {}", configFile.getAbsolutePath());
                throw new IllegalStateException("Config file does not exist: " + configFile.getAbsolutePath());
            }
            LOGGER.info("Loading YAML from: {}", configFile.getAbsolutePath());
            return YamlUtils.loadYaml(configFile);
        } catch (IOException e) {
            LOGGER.error("Failed to load config.yml: {}", configFile.getAbsolutePath(), e);
            throw new IllegalStateException("Failed to load config.yml", e);
        }
    }

    private void createDefaultConfig(File configFile) {
        LOGGER.info("Creating default config at: {}", configFile.getAbsolutePath());
        String defaultContent = """
                plugin:
                  enabled: true
                  saveFilepath: player.json
                  useSql: true
                  migrate: false
                database:
                  url: jdbc:mariadb://localhost:3306/mydb
                  user: user
                  password: user_password
                  driver: org.mariadb.jdbc.Driver
                  dialect: org.hibernate.dialect.MariaDBDialect
                  hbm2ddl: validate
                  showSql: true
                  formatSql: true
                """;
        try {
            if (configFile.exists()) {
                LOGGER.info("Config file already exists, skipping creation: {}", configFile.getAbsolutePath());
                return;
            }
            YamlUtils.createDefaultYaml(configFile, defaultContent);
            LOGGER.info("Successfully created default config.yml");
        } catch (IOException e) {
            LOGGER.error("Failed to create default config.yml: {}", configFile.getAbsolutePath(), e);
            throw new IllegalStateException("Failed to create default config.yml", e);
        }
    }
}