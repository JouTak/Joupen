package integration.server.bukkitTest;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import jakarta.persistence.EntityManager;
import org.bukkit.event.player.PlayerLoginEvent;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
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
        EntityManager em = plugin.getDatabaseManager().getEntityManager();
        assertNotNull(em, "EntityManager не должен быть null");
        assertTrue(em.isOpen(), "EntityManager должен быть открыт");
        em.getTransaction().begin();
        PlayerEntity playerEntity = em.createQuery("SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                .setParameter("name", playerName)
                .getSingleResult();
        assertNotNull(playerEntity);
        assertEquals(playerName, playerEntity.getName());
        assertTrue(playerEntity.getPaid());
        assertNotNull(playerEntity.getLastProlongDate());
        assertTrue(playerEntity.getValidUntil().isAfter(LocalDateTime.now()));
        em.getTransaction().commit();
        em.close();
    }

    @Test
    void testPlayerLoginWithValidSubscription() throws UnknownHostException {
        PlayerMock admin = createAdminPlayer();
        String playerName = "ValidPlayer";
        server.dispatchCommand(admin, "joupen prolong " + playerName + " 30d");

        PlayerMock player = createPlayer(playerName);
        var event = new PlayerLoginEvent(player, "localhost", InetAddress.getByName("127.0.0.1")); // Fixed line

        server.getPluginManager().callEvent(event);

        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());
    }

    @Test
    void testPlayerLoginWithExpiredSubscription() throws UnknownHostException {
        PlayerMock admin = createAdminPlayer();
        String playerName = "ExpiredPlayer";
        server.dispatchCommand(admin, "joupen prolong " + playerName + " 1d");
        EntityManager em = plugin.getDatabaseManager().getEntityManager();
        assertTrue(em.isOpen(), "EntityManager должен быть открыт");
        em.getTransaction().begin();
        PlayerEntity playerEntity = em.createQuery("SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                .setParameter("name", playerName)
                .getSingleResult();
        playerEntity.setValidUntil(LocalDateTime.now().minusDays(1));
        em.merge(playerEntity);
        em.getTransaction().commit();
        em.close();
        PlayerMock player = createPlayer(playerName);
        var event = new PlayerLoginEvent(player, "localhost", InetAddress.getByName("127.0.0.1")); // Fixed for consistency
        server.getPluginManager().callEvent(event);
        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, event.getResult());
        assertTrue(event.getKickMessage().contains("Проходка кончилась"));
    }
}