package org.joupen.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerEntity {
    private Long id;
    private UUID uuid;
    private String name;
    private LocalDateTime validUntil;
    private LocalDateTime lastProlongDate;
    private Boolean paid;
    private Boolean approved;
    private LocalDateTime temporaryAccessFrom;
    private LocalDateTime temporaryAccessUntil;

    public PlayerEntity(Long id, UUID uuid, String name, LocalDateTime validUntil, LocalDateTime lastProlongDate,
                        Boolean paid) {
        this(id, uuid, name, validUntil, lastProlongDate, paid, false, null, null);
    }

    public PlayerEntity(String name, boolean paid, UUID uuid, LocalDateTime validUntil, LocalDateTime lastProlongDate) {
        this(null, uuid, name, validUntil, lastProlongDate, paid, false, null, null);
    }
}
