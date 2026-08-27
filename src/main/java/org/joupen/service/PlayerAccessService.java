package org.joupen.service;

import org.joupen.domain.PlayerEntity;

import java.time.LocalDateTime;
import java.util.Optional;

public class PlayerAccessService {

    public AccessDecision evaluate(Optional<PlayerEntity> player, boolean hasPlayedBefore, LocalDateTime now) {
        if (player.isEmpty() || !Boolean.TRUE.equals(player.get().getApproved())) {
            return AccessDecision.APPROVAL_REQUIRED;
        }

        PlayerEntity entity = player.get();
        if (isPassActive(entity, now) || hasPlayedBefore && isTemporaryAccessActive(entity, now)) {
            return AccessDecision.ALLOWED;
        }

        return AccessDecision.PASS_REQUIRED;
    }

    private boolean isPassActive(PlayerEntity player, LocalDateTime now) {
        return player.getValidUntil() != null && player.getValidUntil().isAfter(now);
    }

    private boolean isTemporaryAccessActive(PlayerEntity player, LocalDateTime now) {
        LocalDateTime from = player.getTemporaryAccessFrom();
        LocalDateTime until = player.getTemporaryAccessUntil();
        return from != null && until != null && !from.isAfter(now) && until.isAfter(now);
    }
}
