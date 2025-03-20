package org.joutak.loginpluginforjoutak.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.joutak.loginpluginforjoutak.utils.JoutakProperties;

import java.util.Collections;
import java.util.Properties;

public class DatabaseManager {

    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);
    private static final String ERROR_USE_SQL_FALSE = "DatabaseManager is only available when useSql is true. Current value: {}";
    private static final String ERROR_CONFIG_NOT_SET = "Database configuration is not properly set in config.yml";
    private static final String ERROR_EMF_CREATION_FAILED = "Failed to create EntityManagerFactory";
    private static final String INFO_INITIALIZING_EMF = "Initializing EntityManagerFactory with properties: url={}, user={}";
    private static final String INFO_EMF_INITIALIZED = "EntityManagerFactory initialized successfully";
    private static final String ERROR_COMMIT_FAILED = "Transaction commit failed: {}";

    // Hibernate property keys
    private static final String JDBC_URL = "jakarta.persistence.jdbc.url";
    private static final String JDBC_USER = "jakarta.persistence.jdbc.user";
    private static final String JDBC_PASSWORD = "jakarta.persistence.jdbc.password";
    private static final String JDBC_DRIVER = "jakarta.persistence.jdbc.driver";
    private static final String HIBERNATE_DIALECT = "hibernate.dialect";
    private static final String HIBERNATE_HBM2DDL = "hibernate.hbm2ddl.auto";
    private static final String HIBERNATE_SHOW_SQL = "hibernate.show_sql";
    private static final String HIBERNATE_FORMAT_SQL = "hibernate.format_sql";
    private static final String HIBERNATE_AUTODETECTION = "hibernate.archive.autodetection";
    private static final String HIBERNATE_SEARCH_LISTENERS = "hibernate.search.autoregister_listeners";

    // Persistence unit name
    private static final String PERSISTENCE_UNIT_NAME = "joutakPU";
    private static final String PLAYER_ENTITY_CLASS = "org.joutak.loginpluginforjoutak.domain.PlayerEntity";

    private static EntityManagerFactory emf;
    private final EntityManager em;
    private final EntityTransaction transaction;

    public DatabaseManager() {
        if (!JoutakProperties.useSql) {
            logger.error(ERROR_USE_SQL_FALSE, JoutakProperties.useSql);
            throw new IllegalStateException("DatabaseManager is only available when useSql is true");
        }

        if (JoutakProperties.dbConfig == null || JoutakProperties.dbConfig.isEmpty()) {
            logger.error(ERROR_CONFIG_NOT_SET);
            throw new IllegalStateException(ERROR_CONFIG_NOT_SET);
        }

        if (emf == null) {
            Properties hibernateProps = new Properties();

            // Преобразуем dbConfig в Properties с правильными ключами Hibernate
            JoutakProperties.dbConfig.forEach((key, value) -> {
                String hibernateKey = switch (key) {
                    case "url" -> JDBC_URL;
                    case "user" -> JDBC_USER;
                    case "password" -> JDBC_PASSWORD;
                    case "driver" -> JDBC_DRIVER;
                    case "dialect" -> HIBERNATE_DIALECT;
                    case "hbm2ddl" -> HIBERNATE_HBM2DDL;
                    case "showSql" -> HIBERNATE_SHOW_SQL;
                    case "formatSql" -> HIBERNATE_FORMAT_SQL;
                    default -> key; // Для дополнительных параметров оставляем как есть
                };
                hibernateProps.setProperty(hibernateKey, String.valueOf(value));
            });

            hibernateProps.setProperty(HIBERNATE_AUTODETECTION, "class");
            hibernateProps.setProperty(HIBERNATE_SEARCH_LISTENERS, "false");

            logger.info(INFO_INITIALIZING_EMF,
                    hibernateProps.getProperty(JDBC_URL),
                    hibernateProps.getProperty(JDBC_USER));

            PersistenceUnitInfo persistenceUnitInfo = new CustomPersistenceUnitInfo(
                    PERSISTENCE_UNIT_NAME,
                    Collections.singletonList(PLAYER_ENTITY_CLASS),
                    hibernateProps
            );

            HibernatePersistenceProvider provider = new HibernatePersistenceProvider();
            emf = provider.createContainerEntityManagerFactory(persistenceUnitInfo, hibernateProps);

            if (emf == null) {
                logger.error(ERROR_EMF_CREATION_FAILED);
                throw new IllegalStateException(ERROR_EMF_CREATION_FAILED);
            }
            logger.info(INFO_EMF_INITIALIZED);
        }
        this.em = emf.createEntityManager();
        this.transaction = em.getTransaction();
    }

    public EntityManager getEntityManager() {
        if (!transaction.isActive()) {
            transaction.begin();
        }
        return em;
    }

    public void commit() {
        if (transaction.isActive()) {
            try {
                transaction.commit();
            } catch (Exception e) {
                logger.error(ERROR_COMMIT_FAILED, e.getMessage(), e);
                rollback();
                throw e;
            }
        }
    }

    public void rollback() {
        if (transaction.isActive()) {
            transaction.rollback();
        }
    }

    public void disconnect() {
        if (em != null && em.isOpen()) {
            if (transaction.isActive()) {
                rollback();
            }
            em.close();
        }
    }

    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            emf = null;
        }
    }
}