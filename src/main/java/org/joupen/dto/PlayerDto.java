package org.joupen.dto;

import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class PlayerDto {
    private Long id;

    @NotNull
    private String name;

    private UUID uuid;

    private LocalDateTime lastProlongDate;

    private LocalDateTime validUntil;

    private Boolean paid = true;

    @Builder.Default
    private Boolean approved = false;

    private LocalDateTime temporaryAccessFrom;

    private LocalDateTime temporaryAccessUntil;
}
