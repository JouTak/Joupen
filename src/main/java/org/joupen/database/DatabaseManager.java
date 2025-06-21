package org.joupen.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.joupen.domain.PlayerEntity;
import org.joupen.utils.HibernatePropertiesBuilder;
import org.joupen.utils.JoupenProperties;

import java.util.Properties;

public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);
    private SessionFactory sessionFactory;
    private EntityManager entityManager;
    private EntityTransaction transaction;

    public DatabaseManager() {
        initializeSessionFactory();
        initializeEntityManager();
    }

    private void initializeSessionFactory() {
        try {
            Properties hibernateProps = HibernatePropertiesBuilder.buildFromDbConfig(JoupenProperties.dbConfig);

            Configuration configuration = new Configuration();
            configuration.addAnnotatedClass(PlayerEntity.class);
            configuration.setProperties(hibernateProps);

            sessionFactory = configuration.buildSessionFactory();
            logger.info("SessionFactory initialized successfully");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SessionFactory: " + e.getMessage(), e);
        }
    }

    private void initializeEntityManager() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory is not initialized");
        }
        entityManager = sessionFactory.createEntityManager();
        transaction = entityManager.getTransaction();
    }

    public EntityManager getEntityManager() {
        if (entityManager == null || !entityManager.isOpen()) {
            initializeEntityManager();
        }
        return entityManager;
    }

    public void beginTransaction() {
        if (!transaction.isActive()) {
            transaction.begin();
            logger.debug("Transaction started");
        }
    }

    public void commit() {
        if (transaction.isActive()) {
            try {
                transaction.commit();
                logger.debug("Transaction committed");
            } catch (Exception e) {
                logger.error("Failed to commit transaction: {}", e.getMessage(), e);
                throw new RuntimeException("Transaction commit failed", e);
            }
        }
    }

    public void rollback() {
        if (transaction.isActive()) {
            try {
                transaction.rollback();
                logger.debug("Transaction rolled back");
            } catch (Exception e) {
                logger.error("Failed to rollback transaction: {}", e.getMessage(), e);
            }
        }
    }

    public void close() {
        if (entityManager != null && entityManager.isOpen()) {
            if (transaction.isActive()) {
                rollback();
            }
            entityManager.close();
            logger.info("EntityManager closed");
        }
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            logger.info("SessionFactory closed");
        }
    }
}