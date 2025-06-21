package org.joupen.repository;

import jakarta.persistence.EntityManager;
import org.joupen.database.TransactionManager;
import org.joupen.repository.impl.PlayerRepositoryDbImpl;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.utils.JoupenProperties;

public class PlayerRepositoryFactory {
    public static PlayerRepository getPlayerRepository(EntityManager entityManager, TransactionManager transactionManager) {
        if (JoupenProperties.useSql) {
            if (entityManager == null) {
                throw new IllegalStateException("EntityManager must be provided when useSql is true");
            }
            return new PlayerRepositoryDbImpl(entityManager, transactionManager);
        } else {
            return new PlayerRepositoryFileImpl();
        }
    }
}