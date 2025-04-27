package org.joutak.loginpluginforjoutak.utils;

import java.util.Map;
import java.util.Properties;

public class HibernatePropertiesBuilder {
    public static Properties buildFromDbConfig(Map<String, Object> dbConfig) {
        Properties properties = new Properties();

        if (dbConfig == null || dbConfig.isEmpty()) {
            throw new IllegalStateException("DB config is empty");
        }

        // Логирование для отладки
        System.out.println("dbConfig: " + dbConfig);

        properties.setProperty("jakarta.persistence.jdbc.url", dbConfig.get("url").toString());
        properties.setProperty("jakarta.persistence.jdbc.user", dbConfig.get("user").toString());
        properties.setProperty("jakarta.persistence.jdbc.password", dbConfig.get("password").toString());
        properties.setProperty("jakarta.persistence.jdbc.driver", dbConfig.get("driver").toString());
        properties.setProperty("hibernate.dialect", dbConfig.get("dialect").toString());
        properties.setProperty("hibernate.hbm2ddl.auto", dbConfig.get("hbm2ddl").toString());
        properties.setProperty("hibernate.show_sql", dbConfig.get("showSql").toString());
        properties.setProperty("hibernate.format_sql", dbConfig.get("formatSql").toString());

        // Фиксированные доп. настройки Hibernate
        properties.setProperty("hibernate.archive.autodetection", "class");
        properties.setProperty("hibernate.search.autoregister_listeners", "false");

        // Логирование результата
        System.out.println("Hibernate Properties: " + properties);

        return properties;
    }
}