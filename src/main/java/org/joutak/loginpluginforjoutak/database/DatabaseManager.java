package org.joutak.loginpluginforjoutak.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joutak.loginpluginforjoutak.utils.HibernatePropertiesBuilder;
import org.joutak.loginpluginforjoutak.utils.JoutakProperties;

import java.util.Properties;

public class DatabaseManager {

    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);
    private static final String PERSISTENCE_UNIT_NAME = "joutakPU";

    private static EntityManagerFactory emf;

    public DatabaseManager() {
        validateConfig();

        if (emf == null) {
            initializeEntityManagerFactory();
        }
    }

    private void validateConfig() {
        if (!JoutakProperties.useSql) {
            logger.error("DatabaseManager is only available when useSql is true. Current value: {}", JoutakProperties.useSql);
            throw new IllegalStateException("DatabaseManager is only available when useSql is true");
        }
        if (JoutakProperties.dbConfig == null || JoutakProperties.dbConfig.isEmpty()) {
            logger.error("Database configuration is not properly set in config.yml");
            throw new IllegalStateException("Database configuration is not properly set in config.yml");
        }
    }

    private void initializeEntityManagerFactory() {
        Properties hibernateProps = HibernatePropertiesBuilder.buildFromDbConfig(JoutakProperties.dbConfig);

        // Логирование для отладки
        logger.info("Hibernate Properties: {}", hibernateProps);

        logger.info("Initializing EntityManagerFactory with URL: {}, USER: {}",
                hibernateProps.getProperty("jakarta.persistence.jdbc.url"),
                hibernateProps.getProperty("jakarta.persistence.jdbc.user"));

        emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProps);

        if (emf == null) {
            logger.error("Failed to create EntityManagerFactory");
            throw new IllegalStateException("Failed to create EntityManagerFactory");
        }
        logger.info("EntityManagerFactory initialized successfully");
    }

    public EntityManager getEntityManager() {
        if (emf == null) {
            throw new IllegalStateException("EntityManagerFactory is not initialized");
        }
        return emf.createEntityManager();
    }

    public void rollback(EntityManager em) {
        if (em == null) return;
        EntityTransaction transaction = em.getTransaction();
        if (transaction.isActive()) {
            transaction.rollback();
        }
    }

    public void disconnect(EntityManager em) {
        if (em != null && em.isOpen()) {
            rollback(em);
            em.close();
            logger.info("EntityManager closed");
        }
    }

    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            emf = null;
            logger.info("EntityManagerFactory closed");
        }
    }
}
