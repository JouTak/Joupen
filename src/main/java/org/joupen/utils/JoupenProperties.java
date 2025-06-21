package org.joupen.utils;

import org.joupen.JoupenPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class JoupenProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(JoupenProperties.class);
    private static JoupenPlugin plugin;
    public static String playersFilepath;
    public static Boolean useSql = false; // Значение по умолчанию
    public static Boolean migrate = false; // Значение по умолчанию
    public static Boolean enabled = true; // Значение по умолчанию
    public static Map<String, Object> dbConfig;
    private static boolean initialized = false;

    private JoupenProperties() {
        // Приватный конструктор для предотвращения создания экземпляров
    }

    public static void initialize(JoupenPlugin pluginInstance) {
        if (initialized) {
            LOGGER.warn("JoupenProperties already initialized, skipping");
            return;
        }
        plugin = pluginInstance;
        loadConfig(plugin.getDataFolder());
        initialized = true;
    }

    public static void loadForTests(File configDir) {
        if (initialized) {
            LOGGER.warn("JoupenProperties already initialized, skipping test load");
            return;
        }
        loadConfig(configDir);
        initialized = true;
    }

    private static void loadConfig(File configDir) {
        LOGGER.info("Plugin data folder: {}", configDir.getAbsolutePath());
        FileUtils.ensureDirectoryExists(configDir);
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
        playersFilepath = new File(configDir, (String) pluginConfig.getOrDefault("playersFile", "player.json")).getPath();
        useSql = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("useSql", false)));
        migrate = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("migrate", false)));
        enabled = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("enabled", true)));
        LOGGER.info("Plugin config: playersFilepath={}, enabled={}, useSql={}, migrate={}",
                playersFilepath, enabled, useSql, migrate);

        if (useSql) {
            dbConfig = (Map<String, Object>) config.getOrDefault("database", Map.of());
            LOGGER.info("Database config: {}", dbConfig);
        } else {
            dbConfig = null;
            LOGGER.info("SQL disabled, dbConfig set to null");
        }
    }

    private static Map<String, Object> loadYaml(File configFile) {
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

    private static void createDefaultConfig(File configFile) {
        LOGGER.info("Creating default config at: {}", configFile.getAbsolutePath());
        String defaultContent = """
                plugin:
                  enabled: true
                  playersFile: player.json
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