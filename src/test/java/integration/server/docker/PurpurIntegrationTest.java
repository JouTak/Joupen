package integration.server.docker;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PurpurIntegrationTest extends BasePurpurIntegrationTest {

    @Test
    void testJoupenProlongCommand() {
        String playerName = "TestPlayer";
        executeCommandAsAdmin("joupen prolong " + playerName + " 30d");

        // Проверяем базу данных
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("joutakPU");
        EntityManager em = emf.createEntityManager();
        try {
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
        } finally {
            em.close();
            emf.close();
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
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("joutakPU");
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            PlayerEntity playerEntity = em.createQuery("SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                    .setParameter("name", playerName)
                    .getSingleResult();
            playerEntity.setValidUntil(LocalDateTime.now().minusDays(1));
            em.merge(playerEntity);
            em.getTransaction().commit();
        } finally {
            em.close();
            emf.close();
        }

        // Эмулируем подключение игрока
        simulatePlayerLogin(playerName);

        // Проверяем, что игрок не в белом списке
        String response = rconClient.sendCommand("whitelist list");
        assertFalse(response.contains(playerName), "Игрок не должен быть в белом списке");
    }
}