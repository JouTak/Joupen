package org.joupen.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.function.Consumer;
import java.util.function.Function;

public class TransactionManager {
    private static final Logger logger = LogManager.getLogger(TransactionManager.class);
    private final DatabaseManager databaseManager;

    public TransactionManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void executeInTransaction(Consumer<EntityManager> operation) {
        EntityManager em = databaseManager.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            operation.accept(em);
            tx.commit();
            logger.debug("Транзакция успешно закоммичена");
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("Ошибка транзакции: {}", e.getMessage(), e);
            throw new RuntimeException("Транзакция не удалась", e);
        } finally {
            em.close();
        }
    }

    public <T> T executeInTransactionWithResult(Function<EntityManager, T> operation) {
        EntityManager em = databaseManager.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = operation.apply(em);
            tx.commit();
            logger.debug("Транзакция успешно закоммичена");
            return result;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("Ошибка транзакции: {}", e.getMessage(), e);
            throw new RuntimeException("Транзакция не удалась", e);
        } finally {
            em.close();
        }
    }
}