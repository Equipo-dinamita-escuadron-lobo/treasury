package com.treasury.copy.application.services;

import com.treasury.copy.application.input.ICancelTreasuryCopyPort;
import com.treasury.copy.application.output.ICopyJobLogRepositoryPort;
import com.treasury.copy.domain.enums.CopyEstado;
import com.treasury.copy.domain.exceptions.DuplicateCopyJobException;
import com.treasury.copy.domain.models.CopyJobLog;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyCancelResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Servicio para cancelar un proceso de copia de treasury.
 * REQ-TREASURY-03.
 */
@Service
@RequiredArgsConstructor
public class CancelTreasuryCopyService implements ICancelTreasuryCopyPort {

    private final ICopyJobLogRepositoryPort logRepo;

    @Override
    public CopyCancelResponseDto cancelar(String idProceso) {
        CopyJobLog log = logRepo.buscarPorIdProceso(idProceso)
                .orElseThrow(() -> new DuplicateCopyJobException(idProceso, 0));

        CopyJobLog cancelado = CopyJobLog.builder()
                .idProceso(log.getIdProceso())
                .fase(log.getFase())
                .modulo(log.getModulo())
                .estado(CopyEstado.CANCELADO)
                .fechaInicio(log.getFechaInicio())
                .fechaFin(Instant.now())
                .equivalenciasGeneradas(log.getEquivalenciasGeneradas())
                .build();
        logRepo.guardar(cancelado);

        return CopyCancelResponseDto.builder()
                .estado(CopyEstado.CANCELADO.name())
                .mensaje("Proceso de copia treasury cancelado exitosamente")
                .build();
    }
}
