package org.joupen.repository;

import org.joupen.database.DatabaseManager;
import org.joupen.database.TransactionManager;
import org.joupen.repository.impl.PlayerRepositoryDbImpl;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.utils.JoupenProperties;

public class PlayerRepositoryFactory {
    public static PlayerRepository getPlayerRepository(TransactionManager transactionManager) {
        if (JoupenProperties.useSql) {
            if (transactionManager == null) {
                throw new IllegalStateException("TransactionManager must be provided when useSql is true");
            }
            return new PlayerRepositoryDbImpl(transactionManager);
        } else {
            return new PlayerRepositoryFileImpl();
        }
    }
}