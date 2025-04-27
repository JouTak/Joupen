package org.joutak.loginpluginforjoutak.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "players")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 16)
    private String name;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID uuid;

    @Column(name = "last_prolong_date")
    private LocalDateTime lastProlongDate;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "paid", nullable = false)
    private Boolean paid;
}