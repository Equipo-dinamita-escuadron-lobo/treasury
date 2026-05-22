package com.treasury.copy.infrastructure.adapters.input.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de response para la ejecución de una fase de copia.
 * Contrato uniforme REQ-TREASURY-02.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CopyPhaseResponseDto {

    private String estado;
    private int registrosProcesados;
    private List<CopyEquivalenciaDto> equivalenciasGeneradas;
    private String mensaje;
    private List<String> advertencias;

    /**
     * Datos exportados en modo BACKUP.
     * Presente únicamente cuando el modo es BACKUP — null en DUPLICATE y RESTORE.
     */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Object datosExportados;
}
