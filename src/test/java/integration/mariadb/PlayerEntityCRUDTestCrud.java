package integration.mariadb;

import org.hibernate.Session;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerEntityCRUDTestCrud extends BaseCrudMariaDBTest {

    @Test
    void testCreatePlayer() {
        PlayerEntity player = new PlayerEntity();
        player.setName("TestPlayer");
        player.setUuid(UUID.randomUUID());
        player.setLastProlongDate(LocalDateTime.now());
        player.setValidUntil(LocalDateTime.now().plusDays(30));
        player.setPaid(true);

        try (Session session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            session.persist(player);
            transaction.commit();
        }

        try (Session session = sessionFactory.openSession()) {
            PlayerEntity savedPlayer = session.find(PlayerEntity.class, player.getId());
            assertNotNull(savedPlayer);
            assertEquals("TestPlayer", savedPlayer.getName());
            assertEquals(player.getUuid(), savedPlayer.getUuid());
            assertEquals(player.getPaid(), savedPlayer.getPaid());
        }
    }

    @Test
    void testReadPlayer() {
        PlayerEntity player = new PlayerEntity();
        player.setName("ReadPlayer");
        player.setUuid(UUID.randomUUID());
        player.setPaid(false);

        Long id;
        try (Session session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            id = (Long) session.save(player);
            transaction.commit();
        }

        try (Session session = sessionFactory.openSession()) {
            PlayerEntity foundPlayer = session.find(PlayerEntity.class, id);
            assertNotNull(foundPlayer);
            assertEquals("ReadPlayer", foundPlayer.getName());
            assertEquals(player.getUuid(), foundPlayer.getUuid());
        }
    }

    @Test
    void testUpdatePlayer() {
        PlayerEntity player = new PlayerEntity();
        player.setName("UpdatePlayer");
        player.setUuid(UUID.randomUUID());
        player.setPaid(false);

        Long id;
        try (Session session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            id = (Long) session.save(player);
            transaction.commit();
        }

        try (Session session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            PlayerEntity playerToUpdate = session.find(PlayerEntity.class, id);
            playerToUpdate.setName("UpdatedPlayer");
            playerToUpdate.setPaid(true);
            playerToUpdate.setValidUntil(LocalDateTime.now().plusDays(60));
            session.merge(playerToUpdate);
            transaction.commit();
        }

        try (Session session = sessionFactory.openSession()) {
            PlayerEntity updatedPlayer = session.find(PlayerEntity.class, id);
            assertEquals("UpdatedPlayer", updatedPlayer.getName());
            assertTrue(updatedPlayer.getPaid());
            assertNotNull(updatedPlayer.getValidUntil());
        }
    }

    @Test
    void testDeletePlayer() {
        PlayerEntity player = new PlayerEntity();
        player.setName("DeletePlayer");
        player.setUuid(UUID.randomUUID());
        player.setPaid(true);

        Long id;
        try (Session session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            id = (Long) session.save(player);
            transaction.commit();
        }

        try (Session session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            PlayerEntity playerToDelete = session.find(PlayerEntity.class, id);
            session.remove(playerToDelete);
            transaction.commit();
        }

        try (Session session = sessionFactory.openSession()) {
            PlayerEntity deletedPlayer = session.find(PlayerEntity.class, id);
            assertNull(deletedPlayer);
        }
    }
}