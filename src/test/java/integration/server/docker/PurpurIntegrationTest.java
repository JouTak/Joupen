package integration.server.docker;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class PurpurIntegrationTest extends BasePurpurIntegrationTest {

    @Test
    void testJoupenProlongCommand() {
        String playerName = "TestPlayer";
        executeCommandAsAdmin("joupen prolong " + playerName + " 30d");

        // Проверяем базу данных
        try (EntityManagerFactory emf = createEntityManagerFactory();
             EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            PlayerEntity playerEntity = em.createQuery("SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                    .setParameter("name", playerName)
                    .getSingleResult();
            assertNotNull(playerEntity, "Игрок должен быть создан в базе");
            assertEquals(playerName, playerEntity.getName(), "Имя игрока должно совпадать");
            assertTrue(playerEntity.getPaid(), "Игрок должен быть помечен как оплативший");
            assertNotNull(playerEntity.getLastProlongDate(), "Дата продления не должна быть null");
            assertTrue(playerEntity.getValidUntil().isAfter(LocalDateTime.now()), "Подписка должна быть действительной");
            em.getTransaction().commit();
        }
    }

    @Test
    void testPlayerLoginWithValidSubscription() {
        String playerName = "ValidPlayer";
        executeCommandAsAdmin("joupen prolong " + playerName + " 30d");

        // Эмулируем подключение игрока
        simulatePlayerLogin(playerName);

        // Проверяем, что игрок в белом списке
        String response = rconClient.sendCommand("whitelist list");
        assertTrue(response.contains(playerName), "Игрок должен быть в белом списке");
    }

    @Test
    void testPlayerLoginWithExpiredSubscription() {
        String playerName = "ExpiredPlayer";
        executeCommandAsAdmin("joupen prolong " + playerName + " 1d");

        // Устанавливаем истёкшую подписку
        try (EntityManagerFactory emf = createEntityManagerFactory();
             EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            PlayerEntity playerEntity = em.createQuery("SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                    .setParameter("name", playerName)
                    .getSingleResult();
            playerEntity.setValidUntil(playerEntity.getValidUntil().minusDays(2));
            em.merge(playerEntity);
            em.getTransaction().commit();
        }

        // Эмулируем подключение игрока
        simulatePlayerLogin(playerName);

        // Проверяем, что игрок не в белом списке
        String response = rconClient.sendCommand("whitelist list");
        assertFalse(response.contains(playerName), "Игрок не должен быть в белом списке");
    }

    private EntityManagerFactory createEntityManagerFactory() {
        Properties properties = new Properties();
        properties.setProperty("jakarta.persistence.jdbc.url", mariaDB.getJdbcUrl());
        properties.setProperty("jakarta.persistence.jdbc.user", mariaDB.getUsername());
        properties.setProperty("jakarta.persistence.jdbc.password", mariaDB.getPassword());
        properties.setProperty("jakarta.persistence.jdbc.driver", mariaDB.getDriverClassName());
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.format_sql", "true");
        return Persistence.createEntityManagerFactory("joutakPU", properties);
    }
}