package org.joupen.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PlayerEntity {
    private Long id;
    private UUID uuid;
    private String name;
    private LocalDateTime validUntil;
    private LocalDateTime lastProlongDate;
    private Boolean paid;
}