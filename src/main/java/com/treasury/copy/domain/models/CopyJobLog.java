package com.treasury.copy.domain.models;

import com.treasury.copy.domain.enums.CopyEstado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio que representa el registro de idempotencia de un trabajo de copia.
 * REQ-TREASURY-01, ADR-38.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CopyJobLog {

    private UUID idProceso;
    private Integer fase;
    private String modulo;
    private CopyEstado estado;
    private Instant fechaInicio;
    private Instant fechaFin;

    @Builder.Default
    private Integer equivalenciasGeneradas = 0;

    private String errorMessage;
}
