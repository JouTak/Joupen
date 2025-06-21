package integration.server.bukkitTest;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.event.player.PlayerLoginEvent;
import org.joupen.database.DatabaseManager;
import org.joupen.database.TransactionManager;
import org.joupen.domain.PlayerEntity;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PluginIntegrationMockBukkitTest extends BasePluginIntegrationTest {

    @Test
    void testJoupenProlongCommand() {
        PlayerMock admin = createAdminPlayer();
        capturedMessages.clear();
        String playerName = "TestPlayer";
        server.dispatchCommand(admin, "joupen prolong " + playerName + " 30d");

        // Получение DatabaseManager и создание TransactionManager
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        TransactionManager transactionManager = new TransactionManager(databaseManager);

        // Проверка данных в базе с использованием TransactionManager
        transactionManager.executeInTransaction(em -> {
            PlayerEntity playerEntity = em.createQuery("SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                    .setParameter("name", playerName)
                    .getSingleResult();
            assertNotNull(playerEntity, "PlayerEntity не должен быть null");
            assertEquals(playerName, playerEntity.getName());
            assertTrue(playerEntity.getPaid(), "Игрок должен быть помечен как платный");
            assertNotNull(playerEntity.getLastProlongDate(), "LastProlongDate не должен быть null");
            assertTrue(playerEntity.getValidUntil().isAfter(LocalDateTime.now()), "ValidUntil должен быть в будущем");
        });
    }

    @Test
    void testPlayerLoginWithValidSubscription() throws UnknownHostException {
        PlayerMock admin = createAdminPlayer();
        String playerName = "ValidPlayer";
        server.dispatchCommand(admin, "joupen prolong " + playerName + " 2d");

        PlayerMock player = createPlayer(playerName);
        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", InetAddress.getByName("127.0.0.1"));

        server.getPluginManager().callEvent(event);

        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());
    }

    @Test
    void testPlayerLoginWithExpiredSubscription() throws UnknownHostException {
        PlayerMock admin = createAdminPlayer();
        String playerName = "ExpiredPlayer";
        server.dispatchCommand(admin, "joupen prolong " + playerName + " 1d");

        // Получение DatabaseManager и создание TransactionManager
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        TransactionManager transactionManager = new TransactionManager(databaseManager);

        // Обновление ValidUntil для имитации истекшей подписки
        transactionManager.executeInTransaction(em -> {
            PlayerEntity playerEntity = em.createQuery("SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                    .setParameter("name", playerName)
                    .getSingleResult();
            playerEntity.setValidUntil(LocalDateTime.now().minusDays(1));
            em.merge(playerEntity);
        });

        PlayerMock player = createPlayer(playerName);
        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", InetAddress.getByName("127.0.0.1"));
        server.getPluginManager().callEvent(event);

        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, event.getResult());
        assertTrue(event.getKickMessage().contains("Проходка кончилась"));
    }
}