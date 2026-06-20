package com.treasury.copy.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de request para ejecutar una fase de copia de treasury.
 * Contrato uniforme REQ-TREASURY-02.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CopyPhaseRequestDto {

    @NotNull
    private UUID idProceso;

    @Positive
    private int fase;

    @NotBlank
    private String entOrigen;

    private String entDestino;

    @NotNull
    private Instant snapshotCorte;

    /**
     * Equivalencias generadas por participantes anteriores.
     * Treasury no tiene FK a CATALOGUE (sin accountId externo),
     * por lo que este campo se ignora en la copia — se acepta por contrato uniforme.
     * REQ-TREASURY-02, ADR-38 (treasury sin remap FK CATALOGUE).
     */
    private List<CopyEquivalenciaDto> equivalenciasPrev;

    /**
     * Datos serializados por un BACKUP previo.
     * Presente únicamente en modo RESTORE — null en DUPLICATE y BACKUP.
     */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Object datosImportados;
}
