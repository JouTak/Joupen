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

    public PlayerEntity(String name, boolean paid, UUID uuid, LocalDateTime validUntil, LocalDateTime lastProlongDate) {
        this.name = name;
        this.paid = paid;
        this.uuid = uuid;
        this.validUntil = validUntil;
        this.lastProlongDate = lastProlongDate;
    }
}