package com.treasury.copy.application.services;

import com.treasury.copy.application.input.IGetTreasuryCopyStatusPort;
import com.treasury.copy.application.output.ICopyJobLogRepositoryPort;
import com.treasury.copy.domain.exceptions.DuplicateCopyJobException;
import com.treasury.copy.domain.models.CopyJobLog;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyStatusResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Servicio para consultar el estado de un proceso de copia de treasury.
 * REQ-TREASURY-03.
 */
@Service
@RequiredArgsConstructor
public class GetTreasuryCopyStatusService implements IGetTreasuryCopyStatusPort {

    private final ICopyJobLogRepositoryPort logRepo;

    @Override
    public CopyStatusResponseDto obtenerEstado(String idProceso) {
        CopyJobLog log = logRepo.buscarPorIdProceso(idProceso)
                .orElseThrow(() -> new DuplicateCopyJobException(idProceso, 0));

        return CopyStatusResponseDto.builder()
                .fase(log.getFase() != null ? log.getFase() : 0)
                .estado(log.getEstado() != null ? log.getEstado().name() : "DESCONOCIDO")
                .registrosProcesados(log.getEquivalenciasGeneradas() != null ? log.getEquivalenciasGeneradas() : 0)
                .intentos(1)
                .ultimoError(log.getErrorMessage())
                .build();
    }
}
