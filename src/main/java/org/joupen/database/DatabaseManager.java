package org.joupen.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.joupen.utils.JoupenProperties;

@Slf4j
public class DatabaseManager {
    private final HikariDataSource dataSource;
    private final DSLContext dslContext;

    public DatabaseManager() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl((String) JoupenProperties.dbConfig.get("url"));
        config.setUsername((String) JoupenProperties.dbConfig.get("user"));
        config.setPassword((String) JoupenProperties.dbConfig.get("password"));
        config.setDriverClassName((String) JoupenProperties.dbConfig.get("driver"));
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
        this.dslContext = DSL.using(dataSource, SQLDialect.MARIADB);
        log.info("DatabaseManager initialized with jOOQ for MariaDB");
    }

    public DSLContext getDslContext() {
        return dslContext;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("DatabaseManager closed");
        }
    }
}