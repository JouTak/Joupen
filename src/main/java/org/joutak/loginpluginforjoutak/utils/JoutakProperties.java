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
    public static String saveFilepath;
    public static final Boolean useSql;
    public static Boolean enabled = Boolean.TRUE;

    public static final Map<String, Object> dbConfig;

    static {
        File configDir = new File("plugins/config/joupen");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        Yaml yaml = new Yaml();
        String configFileName = "config.yml";
        File configFile = new File(configDir, configFileName);

        // Если файл не существует, создаем его с значениями по умолчанию
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }

        Map<String, Object> config = loadYamlConfig(yaml, configFile);

        if (config == null) {
            throw new IllegalStateException("No configuration found in plugins/config/config.yml");
        }

        Map<String, Object> pluginConfig = (Map<String, Object>) config.getOrDefault("plugin", Map.of());
        saveFilepath = "plugins/config/" + pluginConfig.getOrDefault("saveFilepath", "player.json");
        useSql = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("useSql", false)));

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
                            "  saveFilepath: player.json\n" +
                            "  useSql: false\n" +
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