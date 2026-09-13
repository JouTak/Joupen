package org.joupen.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.joupen.utils.JoupenProperties;

@Slf4j
@Getter
public class DatabaseManager implements AutoCloseable {
    private final HikariDataSource dataSource;
    private final DSLContext dslContext;

    public DatabaseManager() {
        java.util.Properties props = new java.util.Properties();
        if (JoupenProperties.dbConfig != null) {
            props.putAll(JoupenProperties.dbConfig);
        }

        HikariConfig config = new HikariConfig(props);

        this.dataSource = new HikariDataSource(config);
        try {
            new DatabaseSchemaManager(dataSource).migrate();
            var settings = new Settings().withRenderNameCase(RenderNameCase.LOWER);
            this.dslContext = DSL.using(dataSource, SQLDialect.MARIADB, settings);
        } catch (RuntimeException e) {
            dataSource.close();
            throw e;
        }
        log.info("DatabaseManager initialized with jOOQ for MariaDB");
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("DatabaseManager closed");
        }
    }
}
