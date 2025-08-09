package org.joupen.database;


import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class TransactionManager {
    private final DatabaseManager databaseManager;

    public TransactionManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void executeInTransaction(Consumer<DSLContext> operation) {
        DSLContext dsl = databaseManager.getDslContext();
        try {
            dsl.transaction(configuration -> {
                DSLContext txDsl = DSL.using(configuration);
                operation.accept(txDsl);
            });
        } catch (DataAccessException e) {
            log.error("Transaction failed: {} {}", e.getMessage(), e);
            throw e;
        }
    }

    public <T> T executeInTransactionWithResult(Function<DSLContext, T> operation) {
        DSLContext dsl = databaseManager.getDslContext();
        try {
            return dsl.transactionResult(configuration -> {
                DSLContext txDsl = DSL.using(configuration);
                return operation.apply(txDsl);
            });
        } catch (DataAccessException e) {
            log.error("Transaction failed: {} {}", e.getMessage(), e);
            throw e;
        }
    }
}