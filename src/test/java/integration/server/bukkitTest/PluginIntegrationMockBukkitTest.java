//package integration.server.bukkitTest;
//
//import be.seeseemelk.mockbukkit.entity.PlayerMock;
//import org.bukkit.event.player.PlayerLoginEvent;
//import org.joupen.database.DatabaseManager;
//import org.joupen.database.TransactionManager;
//import org.joupen.domain.PlayerEntity;
//import org.joupen.jooq.generated.tables.Players;
//import org.junit.jupiter.api.Test;
//
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.time.LocalDateTime;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class PluginIntegrationMockBukkitTest extends BasePluginIntegrationTest {
//
//    @Test
//    void testJoupenProlongCommand() {
//        PlayerMock admin = createAdminPlayer();
//        capturedMessages.clear();
//        String playerName = "TestPlayer";
//        server.dispatchCommand(admin, "joupen prolong " + playerName + " 30d");
//
//        DatabaseManager databaseManager = plugin.getDatabaseManager();
//        TransactionManager transactionManager = new TransactionManager(databaseManager);
//
//        transactionManager.executeInTransactionWithResult(txDsl -> {
//            PlayerEntity playerEntity = txDsl.selectFrom(Players.PLAYERS)
//                    .where(Players.PLAYERS.NAME.eq(playerName))
//                    .fetchOneInto(PlayerEntity.class);
//            assertNotNull(playerEntity, "PlayerEntity не должен быть null");
//            assertEquals(playerName, playerEntity.getName());
//            assertTrue(playerEntity.getPaid(), "Игрок должен быть помечен как платный");
//            assertNotNull(playerEntity.getLastProlongDate(), "LastProlongDate не должен быть null");
//            assertTrue(playerEntity.getValidUntil().isAfter(LocalDateTime.now()), "ValidUntil должен быть в будущем");
//            return null;
//        });
//    }
//
//    @Test
//    void testPlayerLoginWithValidSubscription() throws UnknownHostException {
//        PlayerMock admin = createAdminPlayer();
//        String playerName = "ValidPlayer";
//        server.dispatchCommand(admin, "joupen prolong " + playerName + " 2d");
//
//        PlayerMock player = createPlayer(playerName);
//        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", InetAddress.getByName("127.0.0.1"));
//
//        server.getPluginManager().callEvent(event);
//
//        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());
//    }
//
//    @Test
//    void testPlayerLoginWithExpiredSubscription() throws UnknownHostException {
//        PlayerMock admin = createAdminPlayer();
//        String playerName = "ExpiredPlayer";
//        server.dispatchCommand(admin, "joupen prolong " + playerName + " 1d");
//
//        DatabaseManager databaseManager = plugin.getDatabaseManager();
//        TransactionManager transactionManager = new TransactionManager(databaseManager);
//
//        transactionManager.executeInTransaction(txDsl -> {
//            txDsl.update(Players.PLAYERS)
//                    .set(Players.PLAYERS.VALID_UNTIL, LocalDateTime.now().minusDays(1))
//                    .where(Players.PLAYERS.NAME.eq(playerName))
//                    .execute();
//        });
//
//        PlayerMock player = createPlayer(playerName);
//        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", InetAddress.getByName("127.0.0.1"));
//        server.getPluginManager().callEvent(event);
//
//        assertEquals(PlayerLoginEvent.Result.KICK_WHITELIST, event.getResult());
//        assertTrue(event.getKickMessage().contains("Проходка кончилась"));
//    }
//
//    @Test
//    void testJoupenGiftCommand() {
//        PlayerMock admin = createAdminPlayer();
//        capturedMessages.clear();
//        String playerName = "ExpiredPlayer";
//
//        server.dispatchCommand(admin, "joupen gift " + playerName + " 170d");
//
//        DatabaseManager databaseManager = plugin.getDatabaseManager();
//        TransactionManager transactionManager = new TransactionManager(databaseManager);
//        transactionManager.executeInTransactionWithResult(txDsl -> {
//            PlayerEntity playerEntity = txDsl.selectFrom(Players.PLAYERS)
//                    .where(Players.PLAYERS.NAME.eq(playerName))
//                    .fetchOneInto(PlayerEntity.class);
//            assertNotNull(playerEntity, "Игрок должен быть в базе");
//            assertEquals(playerName, playerEntity.getName(), "Имя игрока должно совпадать");
//            assertTrue(playerEntity.getPaid(), "Игрок должен быть помечен как оплаченный");
//            assertNotNull(playerEntity.getLastProlongDate(), "Дата продления не должна быть null");
//            assertTrue(playerEntity.getValidUntil().isAfter(LocalDateTime.now()), "Дата окончания подписки должна быть в будущем");
//            return null;
//        });
//    }
//}