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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // Despacho por modo: RESTORE → BACKUP → DUPLICATE
        if (request.getDatosImportados() != null) {
            return ejecutarImportacion(request);
        }
        if (request.getEntDestino() == null || request.getEntDestino().isBlank()) {
            return ejecutarExportacion(request);
        }
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

    // ----------------------------------------------------------------
    // BACKUP: exportar datos del tenant origen
    // ----------------------------------------------------------------

    private CopyPhaseResponseDto ejecutarExportacion(CopyPhaseRequestDto request) {
        log.info("Modo BACKUP treasury — exportando datos de entOrigen={}", request.getEntOrigen());

        List<TreasuryEntity> origenList = sourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());

        List<Map<String, Object>> registros = new ArrayList<>();
        for (TreasuryEntity e : origenList) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", e.getId());
            row.put("accountNumber", e.getAccountNumber());
            row.put("balance", e.getBalance());
            row.put("currency", e.getCurrency());
            row.put("accountType", e.getAccountType());
            row.put("status", e.isStatus());
            registros.add(row);
        }

        Map<String, Object> datosExportados = new HashMap<>();
        datosExportados.put("treasury", registros);

        return CopyPhaseResponseDto.builder()
                .estado("COMPLETADO")
                .registrosProcesados(registros.size())
                .equivalenciasGeneradas(Collections.emptyList())
                .mensaje("Modo BACKUP — " + registros.size() + " registros treasury exportados")
                .advertencias(Collections.emptyList())
                .datosExportados(datosExportados)
                .build();
    }

    // ----------------------------------------------------------------
    // RESTORE: importar datos serializados en el tenant destino
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private CopyPhaseResponseDto ejecutarImportacion(CopyPhaseRequestDto request) {
        log.info("Modo RESTORE treasury — importando en entDestino={}", request.getEntDestino());

        String idProceso = request.getIdProceso().toString();

        // Idempotencia
        Optional<CopyJobLog> previo = logRepo.buscarPorIdProcesoYFase(idProceso, request.getFase());
        if (previo.isPresent()) {
            log.info("Fase {} del proceso {} ya fue ejecutada (RESTORE) — idempotencia",
                    request.getFase(), idProceso);
            return construirResponseDesdeLog(previo.get());
        }

        CopyJobLog logInicio = CopyJobLog.builder()
                .idProceso(request.getIdProceso())
                .fase(request.getFase())
                .modulo(MODULO)
                .estado(CopyEstado.EN_PROCESO)
                .fechaInicio(Instant.now())
                .equivalenciasGeneradas(0)
                .build();

        List<String> advertencias = new ArrayList<>();
        List<CopyEquivalenciaDto> equivalencias = new ArrayList<>();

        String tenantOriginal = TenantContext.getTenantId();
        TenantContext.setTenantId(request.getEntDestino());

        int totalRegistros = 0;

        try {
            Map<String, Object> datos = (Map<String, Object>) request.getDatosImportados();
            List<Map<String, Object>> registros = (List<Map<String, Object>>) datos.get("treasury");

            if (registros != null) {
                for (Map<String, Object> row : registros) {
                    Long idOriginal = toLong(row.get("id"));

                    TreasuryEntity nueva = new TreasuryEntity();
                    nueva.setId(null);
                    nueva.setAccountNumber(toStr(row.get("accountNumber")));
                    nueva.setBalance(toDbl(row.get("balance")));
                    nueva.setCurrency(toStr(row.get("currency")));
                    nueva.setAccountType(toStr(row.get("accountType")));
                    nueva.setStatus(toBool(row.get("status")));

                    TreasuryEntity guardada = targetRepo.guardar(nueva);

                    equivalencias.add(CopyEquivalenciaDto.builder()
                            .modulo(MODULO)
                            .tabla(MODULO)
                            .idViejo(String.valueOf(idOriginal))
                            .idNuevo(String.valueOf(guardada.getId()))
                            .build());
                    totalRegistros++;
                }
            }

        } catch (Exception e) {
            log.error("Error inesperado durante RESTORE treasury proceso {}: {}", idProceso, e.getMessage(), e);
            registrarFallo(request, e.getMessage(), logInicio.getFechaInicio());
            return CopyPhaseResponseDto.builder()
                    .estado("ERROR_REINTENTABLE")
                    .mensaje("Error interno RESTORE: " + e.getMessage())
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
                .mensaje("RESTORE treasury completado exitosamente")
                .advertencias(advertencias)
                .build();
    }

    // ----------------------------------------------------------------
    // Helpers de conversión de tipos (JSON deserializado como Object)
    // ----------------------------------------------------------------

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        return null;
    }

    private String toStr(Object v) { return v != null ? v.toString() : null; }

    private boolean toBool(Object v) { return v instanceof Boolean b && b; }

    private double toDbl(Object v) {
        if (v instanceof Double d) return d;
        if (v instanceof Number n) return n.doubleValue();
        return 0.0;
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
