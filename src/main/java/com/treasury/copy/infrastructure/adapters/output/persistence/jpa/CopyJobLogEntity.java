package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.domain.enums.CopyEstado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entidad JPA para el log de idempotencia de copia de treasury.
 * DDL PostgreSQL: copy_job_log con TIMESTAMPTZ + UNIQUE (id_proceso, fase, modulo).
 * REQ-TREASURY-01, ADR-38.
 */
@Entity
@Table(name = "treasury_copy_job_log",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_treasury_copy_job_log",
                columnNames = {"id_proceso", "fase", "modulo"})
    },
    indexes = {
        @Index(name = "idx_treasury_copy_job_log_id_proceso", columnList = "id_proceso")
    }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CopyJobLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_proceso", nullable = false, length = 36)
    private String idProceso;

    @Column(nullable = false)
    private Integer fase;

    @Column(nullable = false, length = 64)
    private String modulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CopyEstado estado;

    @Column(name = "fecha_inicio")
    private Instant fechaInicio;

    @Column(name = "fecha_fin")
    private Instant fechaFin;

    @Column(name = "equivalencias_generadas")
    @Builder.Default
    private Integer equivalenciasGeneradas = 0;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
