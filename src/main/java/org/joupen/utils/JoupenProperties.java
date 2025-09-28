package org.joupen.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
public final class JoupenProperties {

    public static String playersFilepath;
    public static Boolean useSql = false;
    public static Boolean migrate = false;
    public static Boolean enabled = true;
    public static Map<String, Object> dbConfig;
    public static boolean isInitialized = false;

    private JoupenProperties() {
    }

    public static void initialize(File pluginFolder) {
        if (isInitialized) return;
        loadConfig(pluginFolder);
        isInitialized = true;
    }

    /**
     * Инициализация напрямую из Map (например, в тестах)
     */
    public static void initialize(Map<String, Object> config) {
        if (isInitialized) return;
        applyConfig(config, new File("."));
        isInitialized = true;
    }

    private static void loadConfig(File configDir) {
        log.info("Plugin data folder: {}", configDir.getAbsolutePath());

        FileUtils.ensureDirectoryExists(configDir);
        File configFile = new File(configDir, "config.yml");
        log.info("Checking config file: {}", configFile.getAbsolutePath());

        if (!configFile.exists()) {
            log.info("Config file does not exist, creating default config.yml");
            createDefaultConfig(configFile);
        } else {
            log.info("Config file already exists: {}", configFile.getAbsolutePath());
        }

        Map<String, Object> config = loadYaml(configFile);
        log.info("Loaded config:\n{}", config);

        applyConfig(config, configDir);
    }

    @SuppressWarnings("unchecked")
    private static void applyConfig(Map<String, Object> config, File configDir) {
        Map<String, Object> pluginConfig = (Map<String, Object>) config.getOrDefault("plugin", Map.of());

        playersFilepath = new File(configDir, (String) pluginConfig.getOrDefault("playersFile", "player.json")).getPath();

        useSql = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("useSql", false)));
        migrate = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("migrate", false)));
        enabled = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("enabled", true)));

        log.info("Plugin config: playersFilepath={}, enabled={}, useSql={}, migrate={}", playersFilepath, enabled, useSql, migrate);

        if (useSql) {
            dbConfig = (Map<String, Object>) config.getOrDefault("database", Map.of());
            log.info("Database config: {}", dbConfig);
        } else {
            dbConfig = null;
            log.info("SQL disabled, dbConfig set to null");
        }
    }

    private static Map<String, Object> loadYaml(File configFile) {
        try {
            if (!configFile.exists()) {
                log.warn("Config file does not exist: {}", configFile.getAbsolutePath());
                throw new IllegalStateException("Config file does not exist: " + configFile.getAbsolutePath());
            }
            log.info("Loading YAML from: {}", configFile.getAbsolutePath());

            return YamlUtils.loadYaml(configFile);
        } catch (IOException e) {
            log.error("Failed to load config.yml: {} - {}", configFile.getAbsolutePath(), e.getMessage());
            throw new IllegalStateException("Failed to load config.yml", e);
        }
    }

    private static void createDefaultConfig(File configFile) {
        log.info("Creating default config at: {}", configFile.getAbsolutePath());
        String defaultContent = """
                plugin:
                  enabled: true
                  playersFile: player.json
                  useSql: false
                  migrate: false
                database:
                  url: jdbc:mariadb://localhost:3306/mydb
                  user: user
                  password: user_password
                  driver: org.mariadb.jdbc.Driver
                """;
        try {
            if (configFile.exists()) {
                log.info("Config file already exists, skipping creation: {}", configFile.getAbsolutePath());
                return;
            }
            YamlUtils.createDefaultYaml(configFile, defaultContent);
            log.info("Successfully created default config.yml");
        } catch (IOException e) {
            log.error("Failed to create default config.yml: {} - {}", configFile.getAbsolutePath(), e.getMessage());
            throw new IllegalStateException("Failed to create default config.yml", e);
        }
    }
}
