package org.joupen.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastProlongDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validUntil;

    private Boolean paid = true;
}
