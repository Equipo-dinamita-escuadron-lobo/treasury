package com.treasury.copy.application.output;

import com.treasury.copy.domain.models.CopyJobLog;

import java.util.Optional;

/**
 * Puerto de salida: persistencia del log de idempotencia de copia de treasury.
 * REQ-TREASURY-01.
 */
public interface ICopyJobLogRepositoryPort {

    CopyJobLog guardar(CopyJobLog log);

    Optional<CopyJobLog> buscarPorIdProcesoYFase(String idProceso, int fase);

    Optional<CopyJobLog> buscarPorIdProceso(String idProceso);

    void eliminarPorIdProceso(String idProceso);
}
