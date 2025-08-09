package org.joupen.repository;

import org.joupen.database.DatabaseManager;
import org.joupen.database.TransactionManager;
import org.joupen.repository.impl.PlayerRepositoryDbImpl;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.utils.JoupenProperties;

public class PlayerRepositoryFactory {
    public static PlayerRepository getPlayerRepository(DatabaseManager databaseManager, TransactionManager transactionManager) {
        if (JoupenProperties.useSql) {
            if (databaseManager == null || transactionManager == null) {
                throw new IllegalStateException("DatabaseManager and TransactionManager must be provided when useSql is true");
            }
            return new PlayerRepositoryDbImpl(databaseManager.getDslContext(), transactionManager);
        } else {
            return new PlayerRepositoryFileImpl();
        }
    }
}