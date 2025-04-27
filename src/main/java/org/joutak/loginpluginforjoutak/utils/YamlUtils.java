package org.joutak.loginpluginforjoutak.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class YamlUtils {

    private static final Yaml yaml = new Yaml();

    public static Map<String, Object> loadYaml(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return yaml.load(inputStream);
        }
    }

    public static void createDefaultYaml(File file, String defaultContent) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(defaultContent);
        }
    }
}
