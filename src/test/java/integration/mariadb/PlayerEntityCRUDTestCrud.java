package integration.mariadb;

import org.jooq.impl.DSL;
import org.joupen.domain.PlayerEntity;
import org.joupen.jooq.generated.default_schema.tables.Players;
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

        dslContext.transaction(configuration -> {
            DSL.using(configuration)
                    .insertInto(Players.PLAYERS)
                    .set(Players.PLAYERS.UUID, player.getUuid().toString())
                    .set(Players.PLAYERS.NAME, player.getName())
                    .set(Players.PLAYERS.VALID_UNTIL, player.getValidUntil())
                    .set(Players.PLAYERS.LAST_PROLONG_DATE, player.getLastProlongDate())
                    .set(Players.PLAYERS.PAID, player.getPaid())
                    .execute();
        });

        PlayerEntity savedPlayer = dslContext.selectFrom(Players.PLAYERS)
                .where(Players.PLAYERS.UUID.eq(player.getUuid().toString()))
                .fetchOneInto(PlayerEntity.class);

        assertNotNull(savedPlayer);
        assertEquals("TestPlayer", savedPlayer.getName());
        assertEquals(player.getUuid(), savedPlayer.getUuid());
        assertEquals(player.getPaid(), savedPlayer.getPaid());
    }

    @Test
    void testReadPlayer() {
        PlayerEntity player = new PlayerEntity();
        player.setName("ReadPlayer");
        player.setUuid(UUID.randomUUID());
        player.setPaid(false);

        Long id = dslContext.transactionResult(configuration -> DSL.using(configuration)
                .insertInto(Players.PLAYERS)
                .set(Players.PLAYERS.UUID, player.getUuid().toString())
                .set(Players.PLAYERS.NAME, player.getName())
                .set(Players.PLAYERS.PAID, player.getPaid())
                .returning(Players.PLAYERS.ID)
                .fetchOne()
                .getId());

        PlayerEntity foundPlayer = dslContext.selectFrom(Players.PLAYERS)
                .where(Players.PLAYERS.ID.eq(id))
                .fetchOneInto(PlayerEntity.class);

        assertNotNull(foundPlayer);
        assertEquals("ReadPlayer", foundPlayer.getName());
        assertEquals(player.getUuid(), foundPlayer.getUuid());
    }

    @Test
    void testUpdatePlayer() {
        PlayerEntity player = new PlayerEntity();
        player.setName("UpdatePlayer");
        player.setUuid(UUID.randomUUID());
        player.setPaid(false);

        Long id = dslContext.transactionResult(configuration -> DSL.using(configuration)
                .insertInto(Players.PLAYERS)
                .set(Players.PLAYERS.UUID, player.getUuid().toString())
                .set(Players.PLAYERS.NAME, player.getName())
                .set(Players.PLAYERS.PAID, player.getPaid())
                .returning(Players.PLAYERS.ID)
                .fetchOne()
                .getId());

        dslContext.transaction(configuration -> {
            DSL.using(configuration)
                    .update(Players.PLAYERS)
                    .set(Players.PLAYERS.NAME, "UpdatedPlayer")
                    .set(Players.PLAYERS.PAID, true) // true
                    .set(Players.PLAYERS.VALID_UNTIL, LocalDateTime.now().plusDays(60))
                    .where(Players.PLAYERS.ID.eq(id))
                    .execute();
        });

        PlayerEntity updatedPlayer = dslContext.selectFrom(Players.PLAYERS)
                .where(Players.PLAYERS.ID.eq(id))
                .fetchOneInto(PlayerEntity.class);

        assertEquals("UpdatedPlayer", updatedPlayer.getName());
        assertTrue(updatedPlayer.getPaid());
        assertNotNull(updatedPlayer.getValidUntil());
    }

    @Test
    void testDeletePlayer() {
        PlayerEntity player = new PlayerEntity();
        player.setName("DeletePlayer");
        player.setUuid(UUID.randomUUID());
        player.setPaid(true);

        Long id = dslContext.transactionResult(configuration -> DSL.using(configuration)
                .insertInto(Players.PLAYERS)
                .set(Players.PLAYERS.UUID, player.getUuid().toString())
                .set(Players.PLAYERS.NAME, player.getName())
                    .set(Players.PLAYERS.PAID, player.getPaid())
                .returning(Players.PLAYERS.ID)
                .fetchOne()
                .getId());

        dslContext.transaction(configuration -> {
            DSL.using(configuration)
                    .deleteFrom(Players.PLAYERS)
                    .where(Players.PLAYERS.ID.eq(id))
                    .execute();
        });

        PlayerEntity deletedPlayer = dslContext.selectFrom(Players.PLAYERS)
                .where(Players.PLAYERS.ID.eq(id))
                .fetchOneInto(PlayerEntity.class);

        assertNull(deletedPlayer);
    }
}