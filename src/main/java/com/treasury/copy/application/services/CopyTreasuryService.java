package com.treasury.copy.application.services;

import com.treasury.copy.application.input.IExecuteTreasuryCopyPhasePort;
import com.treasury.copy.application.output.ICopyJobLogRepositoryPort;
import com.treasury.copy.application.output.ITreasurySourceRepositoryPort;
import com.treasury.copy.application.output.ITreasuryTargetRepositoryPort;
import com.treasury.copy.domain.enums.CopyEstado;
import com.treasury.copy.domain.models.CopyJobLog;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyEquivalenciaDto;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyPhaseRequestDto;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyPhaseResponseDto;
import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación que orquesta la copia del módulo treasury.
 *
 * Treasury no tiene FK a CATALOGUE (el campo accountNumber es String, no ID).
 * Por lo tanto, no se requiere remapeo de FKs externas — equivalenciasPrev se
 * acepta por contrato uniforme pero se ignora en la lógica de copia.
 *
 * AMQP: durante la copia el flag de instancia copyEnCurso suprime mensajes entrantes
 * gestionado en TreasuryListener mediante CopyActiveFlag.
 *
 * REQ-TREASURY-01, REQ-TREASURY-02, ADR-38.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CopyTreasuryService implements IExecuteTreasuryCopyPhasePort {

    private static final String MODULO = "treasury";

    private final ICopyJobLogRepositoryPort logRepo;
    private final ITreasurySourceRepositoryPort sourceRepo;
    private final ITreasuryTargetRepositoryPort targetRepo;

    @Override
    public CopyPhaseResponseDto ejecutar(CopyPhaseRequestDto request) {
        // Validación básica
        if (request.getEntOrigen().equals(request.getEntDestino())) {
            return CopyPhaseResponseDto.builder()
                    .estado("ERROR_NO_REINTENTABLE")
                    .mensaje("entOrigen y entDestino no pueden ser iguales")
                    .equivalenciasGeneradas(Collections.emptyList())
                    .advertencias(Collections.emptyList())
                    .build();
        }

        String idProceso = request.getIdProceso().toString();

        // Idempotencia
        Optional<CopyJobLog> previo = logRepo.buscarPorIdProcesoYFase(idProceso, request.getFase());
        if (previo.isPresent()) {
            log.info("Fase {} del proceso {} ya fue ejecutada — retornando resultado previo (idempotencia)",
                    request.getFase(), idProceso);
            return construirResponseDesdeLog(previo.get());
        }

        // Registrar inicio
        CopyJobLog logInicio = CopyJobLog.builder()
                .idProceso(request.getIdProceso())
                .fase(request.getFase())
                .modulo(MODULO)
                .estado(CopyEstado.EN_PROCESO)
                .fechaInicio(Instant.now())
                .equivalenciasGeneradas(0)
                .build();
        logRepo.guardar(logInicio);

        List<String> advertencias = new ArrayList<>();
        List<CopyEquivalenciaDto> equivalencias = new ArrayList<>();

        // Tenant override: forzar entDestino para que Hibernate aplique @TenantId correcto
        String tenantOriginal = TenantContext.getTenantId();
        TenantContext.setTenantId(request.getEntDestino());

        int totalRegistros = 0;

        try {
            List<TreasuryEntity> origenList = sourceRepo.findByEntOrigenBeforeSnapshot(
                    request.getEntOrigen(), request.getSnapshotCorte());

            for (TreasuryEntity original : origenList) {
                TreasuryEntity nueva = new TreasuryEntity();
                nueva.setId(null);
                nueva.setAccountNumber(original.getAccountNumber());
                nueva.setBalance(original.getBalance());
                nueva.setCurrency(original.getCurrency());
                nueva.setAccountType(original.getAccountType());
                nueva.setStatus(original.isStatus());
                // tenantId lo aplica Hibernate por @TenantId con el tenant activo (entDestino)

                TreasuryEntity guardada = targetRepo.guardar(nueva);

                equivalencias.add(CopyEquivalenciaDto.builder()
                        .modulo(MODULO)
                        .tabla(MODULO)
                        .idViejo(String.valueOf(original.getId()))
                        .idNuevo(String.valueOf(guardada.getId()))
                        .build());
                totalRegistros++;
            }

        } catch (Exception e) {
            log.error("Error inesperado durante copia treasury del proceso {}: {}", idProceso, e.getMessage(), e);
            registrarFallo(request, e.getMessage(), logInicio.getFechaInicio());
            return CopyPhaseResponseDto.builder()
                    .estado("ERROR_REINTENTABLE")
                    .mensaje("Error interno: " + e.getMessage())
                    .equivalenciasGeneradas(Collections.emptyList())
                    .advertencias(Collections.emptyList())
                    .build();
        } finally {
            if (tenantOriginal != null) {
                TenantContext.setTenantId(tenantOriginal);
            } else {
                TenantContext.clear();
            }
        }

        CopyEstado estadoFinal = advertencias.isEmpty()
                ? CopyEstado.COMPLETADO
                : CopyEstado.COMPLETADO_CON_ADVERTENCIAS;

        CopyJobLog logFin = CopyJobLog.builder()
                .idProceso(request.getIdProceso())
                .fase(request.getFase())
                .modulo(MODULO)
                .estado(estadoFinal)
                .fechaInicio(logInicio.getFechaInicio())
                .fechaFin(Instant.now())
                .equivalenciasGeneradas(equivalencias.size())
                .build();
        logRepo.guardar(logFin);

        return CopyPhaseResponseDto.builder()
                .estado(estadoFinal.name())
                .registrosProcesados(totalRegistros)
                .equivalenciasGeneradas(equivalencias)
                .mensaje("Copia treasury completada exitosamente")
                .advertencias(advertencias)
                .build();
    }

    private CopyPhaseResponseDto construirResponseDesdeLog(CopyJobLog log) {
        return CopyPhaseResponseDto.builder()
                .estado(log.getEstado().name())
                .registrosProcesados(log.getEquivalenciasGeneradas() != null ? log.getEquivalenciasGeneradas() : 0)
                .equivalenciasGeneradas(Collections.emptyList())
                .mensaje("Resultado de ejecución previa (idempotencia)")
                .advertencias(Collections.emptyList())
                .build();
    }

    private void registrarFallo(CopyPhaseRequestDto request, String mensaje, Instant fechaInicio) {
        try {
            CopyJobLog logFallo = CopyJobLog.builder()
                    .idProceso(request.getIdProceso())
                    .fase(request.getFase())
                    .modulo(MODULO)
                    .estado(CopyEstado.FALLIDO)
                    .fechaInicio(fechaInicio != null ? fechaInicio : Instant.now())
                    .fechaFin(Instant.now())
                    .equivalenciasGeneradas(0)
                    .errorMessage(mensaje)
                    .build();
            logRepo.guardar(logFallo);
        } catch (Exception e) {
            log.error("Error al registrar fallo de copia treasury: {}", e.getMessage());
        }
    }
}
