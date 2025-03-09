package org.joutak.loginpluginforjoutak.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class JoutakProperties {

    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String profile;

    // Для профиля с файлом (не prod)
    public static String saveFilepath;

    // Для профиля с базой данных (prod)
    public static final String dbUrl;
    public static final String dbUser;
    public static final String dbPassword;
    public static final String dbDriver;
    public static final String dbDialect;
    public static final String dbHbm2ddl;
    public static final Boolean dbShowSql;
    public static final Boolean dbFormatSql;
    public static Boolean enabled = true;

    static {
        profile = System.getProperty("plugin.profile", "prod");

        Yaml yaml = new Yaml();
        String configFileName = "prod".equals(profile) ? "application.yml" : "application-" + profile + ".yml";
        Map<String, Object> config = loadYamlConfig(yaml, configFileName);
        if (config == null) {
            throw new IllegalStateException("No configuration file found for profile '" + profile + "' (" + configFileName + ")");
        }

        if ("prod".equals(profile)) {
            Map<String, Object> dbConfig = (Map<String, Object>) config.getOrDefault("database", Map.of());
            dbUrl = (String) dbConfig.getOrDefault("url", "jdbc:mariadb://localhost:3306/mydb");
            dbUser = (String) dbConfig.getOrDefault("user", "user");
            dbPassword = (String) dbConfig.getOrDefault("password", "user_password");
            dbDriver = (String) dbConfig.getOrDefault("driver", "org.mariadb.jdbc.Driver");
            dbDialect = (String) dbConfig.getOrDefault("dialect", "org.hibernate.dialect.MariaDBDialect");
            dbHbm2ddl = (String) dbConfig.getOrDefault("hbm2ddl", "update");
            dbShowSql = Boolean.parseBoolean(String.valueOf(dbConfig.getOrDefault("showSql", true)));
            dbFormatSql = Boolean.parseBoolean(String.valueOf(dbConfig.getOrDefault("formatSql", true)));

            Map<String, Object> pluginConfig = (Map<String, Object>) config.getOrDefault("plugin", Map.of());
            saveFilepath = (String) pluginConfig.getOrDefault("saveFilepath", "loginPluginRes/players.json");
        } else {
            Map<String, Object> pluginConfig = (Map<String, Object>) config.getOrDefault("plugin", Map.of());
            saveFilepath = (String) pluginConfig.getOrDefault("saveFilepath", "loginPluginRes/players.json");
            dbUrl = null;
            dbUser = null;
            dbPassword = null;
            dbDriver = null;
            dbDialect = null;
            dbHbm2ddl = null;
            dbShowSql = null;
            dbFormatSql = null;
        }
    }

    private static Map<String, Object> loadYamlConfig(Yaml yaml, String fileName) {
        try (InputStream inputStream = JoutakProperties.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                return yaml.load(inputStream);
            }
        } catch (Exception e) {
            System.err.println("Failed to load " + fileName + ": " + e.getMessage());
        }
        return null;
    }

    private JoutakProperties() {
        throw new UnsupportedOperationException("Utility class");
    }
}