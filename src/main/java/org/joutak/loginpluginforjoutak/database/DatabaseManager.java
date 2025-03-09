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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DatabaseManager {

    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);
    private static EntityManagerFactory emf;
    private EntityManager em;
    private EntityTransaction transaction;

    public DatabaseManager() {
        if (!"prod".equals(JoutakProperties.profile)) {
            logger.error("DatabaseManager is only available in 'prod' profile. Current profile: {}", JoutakProperties.profile);
            throw new IllegalStateException("DatabaseManager is only available in 'prod' profile. Current profile: " + JoutakProperties.profile);
        }

        if (JoutakProperties.dbUrl == null || JoutakProperties.dbUser == null || JoutakProperties.dbPassword == null) {
            logger.error("Database configuration is not properly set: dbUrl={}, dbUser={}, dbPassword={}",
                    JoutakProperties.dbUrl, JoutakProperties.dbUser, JoutakProperties.dbPassword);
            throw new IllegalStateException("Database configuration is not properly set in JoutakProperties for 'prod' profile.");
        }

        if (emf == null) {
            logger.info("Initializing EntityManagerFactory with properties: url={}, user={}",
                    JoutakProperties.dbUrl, JoutakProperties.dbUser);

            Map<String, Object> persistenceProperties = new HashMap<>();
            persistenceProperties.put("jakarta.persistence.jdbc.url", JoutakProperties.dbUrl);
            persistenceProperties.put("jakarta.persistence.jdbc.user", JoutakProperties.dbUser);
            persistenceProperties.put("jakarta.persistence.jdbc.password", JoutakProperties.dbPassword);
            persistenceProperties.put("jakarta.persistence.jdbc.driver", JoutakProperties.dbDriver);
            persistenceProperties.put("hibernate.dialect", JoutakProperties.dbDialect);
            persistenceProperties.put("hibernate.hbm2ddl.auto", JoutakProperties.dbHbm2ddl);
            persistenceProperties.put("hibernate.show_sql", String.valueOf(JoutakProperties.dbShowSql));
            persistenceProperties.put("hibernate.format_sql", String.valueOf(JoutakProperties.dbFormatSql));
            persistenceProperties.put("hibernate.archive.autodetection", "class");
            persistenceProperties.put("hibernate.search.autoregister_listeners", "false");

            Properties props = new Properties();
            persistenceProperties.forEach((key, value) -> props.put(key, value.toString()));
            PersistenceUnitInfo persistenceUnitInfo = new CustomPersistenceUnitInfo(
                    "joutakPU",
                    Collections.singletonList("org.joutak.loginpluginforjoutak.domain.PlayerEntity"),
                    props
            );

            HibernatePersistenceProvider provider = new HibernatePersistenceProvider();
            emf = provider.createContainerEntityManagerFactory(persistenceUnitInfo, persistenceProperties);

            if (emf == null) {
                logger.error("Failed to create EntityManagerFactory");
                throw new IllegalStateException("Failed to create EntityManagerFactory");
            }
            logger.info("EntityManagerFactory initialized successfully");
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
                logger.error("Transaction commit failed: {}", e.getMessage(), e);
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

