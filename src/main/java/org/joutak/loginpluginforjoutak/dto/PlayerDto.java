package org.joutak.loginpluginforjoutak.dto;

import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
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

    private LocalDate lastProlongDate;

    private LocalDate validUntil;

    private boolean isPaid = true;

}
