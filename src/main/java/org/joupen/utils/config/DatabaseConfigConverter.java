package org.joupen.utils.config;

import java.util.Map;

public class DatabaseConfigConverter implements ConfigConverter {

    @Override
    @SuppressWarnings("unchecked")
    public boolean convert(Map<String, Object> config) {
        boolean changed = false;

        if (config.containsKey("database") && config.get("database") instanceof Map) {
            Map<String, Object> dbConfig = (Map<String, Object>) config.get("database");

            if (dbConfig.containsKey("url") && !dbConfig.containsKey("jdbcUrl")) {
                dbConfig.put("jdbcUrl", dbConfig.remove("url"));
                changed = true;
            }
            if (dbConfig.containsKey("user") && !dbConfig.containsKey("username")) {
                dbConfig.put("username", dbConfig.remove("user"));
                changed = true;
            }
            if (dbConfig.containsKey("driver") && !dbConfig.containsKey("driverClassName")) {
                dbConfig.put("driverClassName", dbConfig.remove("driver"));
                changed = true;
            }
            if (dbConfig.containsKey("maxPoolSize") && !dbConfig.containsKey("maximumPoolSize")) {
                dbConfig.put("maximumPoolSize", dbConfig.remove("maxPoolSize"));
                changed = true;
            }
        }

        return changed;
    }
}
