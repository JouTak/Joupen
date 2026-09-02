package org.joupen.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerOperation {
    private Long id;
    private OperationType type;
    private long durationSeconds;
    private LocalDateTime occurredAt;
    private OperationMetadata metadata;
    private String request;
    private Long reversedOperationId;
    private PlayerEntity before;
    private PlayerEntity after;

    public static PlayerEntity snapshot(PlayerEntity player) {
        if (player == null) return null;
        return new PlayerEntity(player.getId(), player.getUuid(), player.getName(), player.getValidUntil(),
                player.getLastProlongDate(), player.getPaid(), player.getApproved(),
                player.getTemporaryAccessFrom(), player.getTemporaryAccessUntil());
    }
}
