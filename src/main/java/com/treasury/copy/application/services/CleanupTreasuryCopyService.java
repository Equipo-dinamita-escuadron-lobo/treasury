package com.treasury.copy.application.services;

import com.treasury.copy.application.input.ICleanupTreasuryCopyPort;
import com.treasury.copy.application.output.ICopyJobLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio para limpiar los registros de log de un proceso de copia de treasury.
 * REQ-TREASURY-03.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CleanupTreasuryCopyService implements ICleanupTreasuryCopyPort {

    private final ICopyJobLogRepositoryPort logRepo;

    @Override
    public void limpiar(String idProceso) {
        log.info("Limpiando registros de copia treasury para proceso {}", idProceso);
        logRepo.eliminarPorIdProceso(idProceso);
    }
}
