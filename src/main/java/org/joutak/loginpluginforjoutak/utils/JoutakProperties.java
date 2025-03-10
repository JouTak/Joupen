package org.joutak.loginpluginforjoutak.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class JoutakProperties {

    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static String saveFilepath;
    public static final Boolean useSql;
    public static Boolean enabled = true;

    // Database configuration as a Map
    public static final Map<String, Object> dbConfig;

    static {
        File configDir = new File("plugins/config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        Yaml yaml = new Yaml();
        String configFileName = "config.yml";
        File configFile = new File(configDir, configFileName);
        Map<String, Object> config = loadYamlConfig(yaml, configFile);

        if (config == null) {
            throw new IllegalStateException("No configuration found in plugins/config/config.yml");
        }

        Map<String, Object> pluginConfig = (Map<String, Object>) config.getOrDefault("plugin", Map.of());
        saveFilepath = "plugins/config/" + pluginConfig.getOrDefault("saveFilepath", "player.json");
        useSql = Boolean.parseBoolean(String.valueOf(pluginConfig.getOrDefault("useSql", true)));

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

    private JoutakProperties() {
        throw new UnsupportedOperationException("Utility class");
    }
}