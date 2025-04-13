package org.joutak.loginpluginforjoutak.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class JoutakProperties {

    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String CONFIG_DIR = "plugins/joupen";
    public static final String CONFIG_FILE = CONFIG_DIR + "/config.yml";
    public static final String DEFAULT_PLAYERS_FILE = "players.json";
    public static String playersFilepath;
    public static final Boolean useSql;
    public static final Boolean migrate;
    public static Boolean enabled = Boolean.TRUE;
    public static final Map<String, Object> dbConfig;

    static {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        Yaml yaml = new Yaml();
        File configFile = new File(CONFIG_FILE);

        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }

        Map<String, Object> config = loadYamlConfig(yaml, configFile);

        if (config == null) {
            throw new IllegalStateException("No configuration found in " + CONFIG_FILE);
        }

        Map<String, Object> pluginConfig = (Map<String, Object>) config.getOrDefault("plugin", Map.of());
        String playersFile = (String) pluginConfig.getOrDefault("playersFile", DEFAULT_PLAYERS_FILE);
        // Проверка на недопустимые пути
        if (playersFile.contains("..") || new File(playersFile).isAbsolute()) {
            throw new IllegalArgumentException("Invalid playersFile path: " + playersFile + ". Path must be relative and within " + CONFIG_DIR);
        }
        playersFilepath = CONFIG_DIR + "/" + playersFile;

        useSql = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("useSql", false)));
        migrate = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("migrate", false)));
        enabled = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("enabled", true)));

        if (useSql) {
            dbConfig = (Map<String, Object>) config.getOrDefault("database", Map.of());
        } else {
            dbConfig = null;
        }
    }

    private static Map<String, Object> loadYamlConfig(Yaml yaml, File configFile) {
        try (InputStream inputStream = new FileInputStream(configFile)) {
            return yaml.load(inputStream);
        } catch (Exception e) {
            System.err.println("Failed to load " + configFile.getPath() + ": " + e.getMessage());
            return null;
        }
    }

    private static void createDefaultConfig(File configFile) {
        try (FileWriter writer = new FileWriter(configFile)) {
            String defaultConfig =
                    "plugin:\n" +
                            "  enabled: true\n" +
                            "  playersFile: players.json\n" +
                            "  useSql: false\n" +
                            "  migrate: false\n" +
                            "database:\n" +
                            "  url: jdbc:mariadb://localhost:3306/mydb\n" +
                            "  user: user\n" +
                            "  password: user_password\n" +
                            "  driver: org.mariadb.jdbc.Driver\n" +
                            "  dialect: org.hibernate.dialect.MariaDBDialect\n" +
                            "  hbm2ddl: update\n" +
                            "  showSql: true\n" +
                            "  formatSql: true\n";
            writer.write(defaultConfig);
            System.out.println("Created default config file at " + configFile.getPath());
        } catch (Exception e) {
            System.err.println("Failed to create default config file: " + e.getMessage());
        }
    }

    private JoutakProperties() {
        throw new UnsupportedOperationException("Utility class");
    }
}