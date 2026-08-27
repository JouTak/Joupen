package service;

import org.joupen.domain.PlayerEntity;
import org.joupen.service.AccessDecision;
import org.joupen.service.PlayerAccessService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerAccessServiceTest {
    private final PlayerAccessService service = new PlayerAccessService();
    private final LocalDateTime now = LocalDateTime.of(2026, 8, 27, 12, 0);

    @Test
    void missingPlayer_shouldRequireApproval() {
        assertEquals(AccessDecision.APPROVAL_REQUIRED, service.evaluate(Optional.empty(), false, now));
    }

    @Test
    void unapprovedPlayerWithActivePass_shouldRequireApproval() {
        PlayerEntity player = approvedPlayer();
        player.setApproved(false);
        player.setValidUntil(now.plusDays(1));

        assertEquals(AccessDecision.APPROVAL_REQUIRED, service.evaluate(Optional.of(player), true, now));
    }

    @Test
    void approvedPlayerWithActivePass_shouldBeAllowed() {
        PlayerEntity player = approvedPlayer();
        player.setValidUntil(now.plusDays(1));

        assertEquals(AccessDecision.ALLOWED, service.evaluate(Optional.of(player), false, now));
    }

    @Test
    void returningPlayerWithTemporaryAccess_shouldBeAllowed() {
        PlayerEntity player = approvedPlayer();
        player.setTemporaryAccessFrom(now.minusHours(1));
        player.setTemporaryAccessUntil(now.plusHours(1));

        assertEquals(AccessDecision.ALLOWED, service.evaluate(Optional.of(player), true, now));
    }

    @Test
    void newPlayerWithTemporaryAccess_shouldRequirePass() {
        PlayerEntity player = approvedPlayer();
        player.setTemporaryAccessFrom(now.minusHours(1));
        player.setTemporaryAccessUntil(now.plusHours(1));

        assertEquals(AccessDecision.PASS_REQUIRED, service.evaluate(Optional.of(player), false, now));
    }

    @Test
    void temporaryAccessBeforeWindow_shouldRequirePass() {
        PlayerEntity player = approvedPlayer();
        player.setTemporaryAccessFrom(now.plusMinutes(1));
        player.setTemporaryAccessUntil(now.plusHours(1));

        assertEquals(AccessDecision.PASS_REQUIRED, service.evaluate(Optional.of(player), true, now));
    }

    @Test
    void temporaryAccessAfterWindow_shouldRequirePass() {
        PlayerEntity player = approvedPlayer();
        player.setTemporaryAccessFrom(now.minusHours(2));
        player.setTemporaryAccessUntil(now);

        assertEquals(AccessDecision.PASS_REQUIRED, service.evaluate(Optional.of(player), true, now));
    }

    private PlayerEntity approvedPlayer() {
        PlayerEntity player = new PlayerEntity();
        player.setApproved(true);
        return player;
    }
}
