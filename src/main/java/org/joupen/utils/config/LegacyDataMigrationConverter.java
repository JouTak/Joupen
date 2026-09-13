package org.joupen.utils.config;

import java.util.Map;

public class LegacyDataMigrationConverter implements ConfigConverter {
    @Override
    @SuppressWarnings("unchecked")
    public boolean convert(Map<String, Object> config) {
        if (!(config.get("plugin") instanceof Map<?, ?> rawPlugin)) return false;
        Map<String, Object> plugin = (Map<String, Object>) rawPlugin;
        if (!plugin.containsKey("migrate")) return false;
        plugin.remove("migrate");
        return true;
    }
}
