package org.joupen.database;

import jakarta.persistence.EntityManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.SessionFactory;
import org.joupen.domain.PlayerEntity;
import org.joupen.utils.HibernatePropertiesBuilder;
import org.joupen.utils.JoupenProperties;

import java.util.Properties;


public class DatabaseManager {

    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);
    private SessionFactory sessionFactory;

    public DatabaseManager() {
        initializeSessionFactory();
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
            //logger.error("Failed to initialize SessionFactory", e);
            throw new IllegalStateException("Failed to initialize SessionFactory: " + e.getMessage(), e);
        }
    }

    public EntityManager getEntityManager() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory is not initialized");
        }
        return sessionFactory.createEntityManager();
    }

    public void close() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            logger.info("SessionFactory closed");
        }
    }
}