package org.joutak.loginpluginforjoutak.repository;

import jakarta.persistence.EntityManager;
import org.joutak.loginpluginforjoutak.repository.impl.PlayerRepositoryDbImpl;
import org.joutak.loginpluginforjoutak.repository.impl.PlayerRepositoryFileImpl;
import org.joutak.loginpluginforjoutak.utils.JoutakProperties;

public class PlayerRepositoryFactory {

    public static PlayerRepository getPlayerRepository(EntityManager entityManager) {
        if (JoutakProperties.useSql) {
            if (entityManager == null) {
                throw new IllegalStateException("EntityManager must be provided when useSql is true");
            }
            return new PlayerRepositoryDbImpl(entityManager);
        } else {
            return new PlayerRepositoryFileImpl();
        }
    }
}