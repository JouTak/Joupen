package org.joupen.utils;

import lombok.extern.slf4j.Slf4j;

import org.joupen.utils.config.ConfigConverter;
import org.joupen.utils.config.DatabaseConfigConverter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public final class JoupenProperties {

    public static String playersFilepath;
    public static Boolean useSql = false;
    public static Boolean migrate = false;
    public static Boolean enabled = true;
    public static Map<String, Object> dbConfig;
    public static boolean discordSrvPassProlongedEnabled = true;
    public static String discordSrvPassProlongedChannel = "mc-live";
    public static String discordSrvPassProlongedColor = "#30d5c8";
    public static boolean discordSrvPassProlongedShowDuration = false;
    public static boolean isInitialized = false;

    private static final List<ConfigConverter> CONVERTERS = List.of(
            new DatabaseConfigConverter());

    private JoupenProperties() {
    }

    public static void initialize(File pluginFolder) {
        if (isInitialized)
            return;
        loadConfig(pluginFolder);
        isInitialized = true;
    }

    /**
     * Инициализация напрямую из Map (например, в тестах)
     */
    public static void initialize(Map<String, Object> config) {
        if (isInitialized)
            return;
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

        boolean changed = false;
        for (ConfigConverter converter : CONVERTERS) {
            if (converter.convert(config)) {
                changed = true;
                log.info("Config migrated by {}", converter.getClass().getSimpleName());
            }
        }

        if (changed) {
            try {
                YamlUtils.saveYaml(configFile, config);
                log.info("Saved migrated config to {}", configFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to save migrated config: {}", e.getMessage(), e);
            }
        }

        log.info("Loaded config:\n{}", config);

        applyConfig(config, configDir);
    }

    @SuppressWarnings("unchecked")
    private static void applyConfig(Map<String, Object> config, File configDir) {
        Map<String, Object> pluginConfig = (Map<String, Object>) config.getOrDefault("plugin", Map.of());

        playersFilepath = new File(configDir, (String) pluginConfig.getOrDefault("playersFile", "player.json"))
                .getPath();

        useSql = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("useSql", false)));
        migrate = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("migrate", false)));
        enabled = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("enabled", true)));

        log.info("Plugin config: playersFilepath={}, enabled={}, useSql={}, migrate={}", playersFilepath, enabled,
                useSql, migrate);

        Map<String, Object> discordSrvConfig = getMap(config, "discordsrv");
        Map<String, Object> passProlongedConfig = getMap(discordSrvConfig, "pass-prolonged");
        discordSrvPassProlongedEnabled = Boolean.parseBoolean(
                String.valueOf(passProlongedConfig.getOrDefault("enabled", true))
        );
        discordSrvPassProlongedChannel = String.valueOf(passProlongedConfig.getOrDefault("channel", "mc-live"));
        discordSrvPassProlongedColor = String.valueOf(passProlongedConfig.getOrDefault("color", "#30d5c8"));
        discordSrvPassProlongedShowDuration = Boolean.parseBoolean(
                String.valueOf(passProlongedConfig.getOrDefault("show-duration", false))
        );
        log.info(
                "DiscordSRV config: enabled={}, channel={}, color={}, showDuration={}",
                discordSrvPassProlongedEnabled,
                discordSrvPassProlongedChannel,
                discordSrvPassProlongedColor,
                discordSrvPassProlongedShowDuration
        );

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
                  jdbcUrl: jdbc:mariadb://localhost:3306/mydb
                  username: user
                  password: user_password
                  driverClassName: org.mariadb.jdbc.Driver
                  maximumPoolSize: 10
                discordsrv:
                  pass-prolonged:
                    enabled: true
                    channel: mc-live
                    color: "#30d5c8"
                    show-duration: false
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
