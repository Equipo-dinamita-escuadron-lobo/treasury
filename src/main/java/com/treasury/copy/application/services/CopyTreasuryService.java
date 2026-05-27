package com.treasury.copy.application.services;

import com.treasury.copy.application.input.IExecuteTreasuryCopyPhasePort;
import com.treasury.copy.application.output.ICopyJobLogRepositoryPort;
import com.treasury.copy.application.output.IPaymentVoucherDetailSourceRepositoryPort;
import com.treasury.copy.application.output.IPaymentVoucherDetailTargetRepositoryPort;
import com.treasury.copy.application.output.IPaymentVoucherSourceRepositoryPort;
import com.treasury.copy.application.output.IPaymentVoucherTargetRepositoryPort;
import com.treasury.copy.application.output.ISplitSourceRepositoryPort;
import com.treasury.copy.application.output.ISplitTargetRepositoryPort;
import com.treasury.copy.application.output.ITransactionSourceRepositoryPort;
import com.treasury.copy.application.output.ITransactionTargetRepositoryPort;
import com.treasury.copy.application.output.ITreasurySourceRepositoryPort;
import com.treasury.copy.application.output.ITreasuryTargetRepositoryPort;
import com.treasury.copy.domain.enums.CopyEstado;
import com.treasury.copy.domain.models.CopyJobLog;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyEquivalenciaDto;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyPhaseRequestDto;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyPhaseResponseDto;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherDetailEntity;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import com.treasury.infrastructure.adapters.output.jpa.entity.SplitEntity;
import com.treasury.infrastructure.adapters.output.jpa.entity.TransactionEntity;
import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio de aplicación que orquesta la copia del módulo treasury.
 *
 * Entidades cubiertas:
 *   1. TreasuryEntity          — sin FK internas
 *   2. TransactionEntity       — sin FK internas
 *   3. PaymentVoucherEntity    — sin FK internas (payeeId/methodId/bankAccountId son FK externas, se copian tal cual)
 *   4. SplitEntity             — FK interna: transactionId → remapeado con equivalencias de TransactionEntity
 *   5. PaymentVoucherDetailEntity — FK interna: voucherId → remapeado con equivalencias de PaymentVoucherEntity
 *
 * FK externas (accountId en Split, factureId en PaymentVoucherDetail,
 * payeeId/methodId/bankAccountId en PaymentVoucher) se copian sin remapeo —
 * son referencias cross-service.
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

    // ---- Treasury ----
    private final ICopyJobLogRepositoryPort logRepo;
    private final ITreasurySourceRepositoryPort sourceRepo;
    private final ITreasuryTargetRepositoryPort targetRepo;

    // ---- Transaction ----
    private final ITransactionSourceRepositoryPort transactionSourceRepo;
    private final ITransactionTargetRepositoryPort transactionTargetRepo;

    // ---- PaymentVoucher ----
    private final IPaymentVoucherSourceRepositoryPort paymentVoucherSourceRepo;
    private final IPaymentVoucherTargetRepositoryPort paymentVoucherTargetRepo;

    // ---- Split ----
    private final ISplitSourceRepositoryPort splitSourceRepo;
    private final ISplitTargetRepositoryPort splitTargetRepo;

    // ---- PaymentVoucherDetail ----
    private final IPaymentVoucherDetailSourceRepositoryPort paymentVoucherDetailSourceRepo;
    private final IPaymentVoucherDetailTargetRepositoryPort paymentVoucherDetailTargetRepo;

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

        String tenantOriginal = TenantContext.getTenantId();
        TenantContext.setTenantId(request.getEntDestino());

        int totalRegistros = 0;

        try {
            // 1. TreasuryEntity
            totalRegistros += copiarTreasuries(request, equivalencias);

            // 2. TransactionEntity (no deps)
            Map<Long, Long> transactionEquiv = new HashMap<>();
            totalRegistros += copiarTransactions(request, equivalencias, transactionEquiv);

            // 3. PaymentVoucherEntity (no deps)
            Map<Long, Long> voucherEquiv = new HashMap<>();
            totalRegistros += copiarPaymentVouchers(request, equivalencias, voucherEquiv);

            // 4. SplitEntity (depende de TransactionEntity)
            totalRegistros += copiarSplits(request, equivalencias, transactionEquiv, advertencias);

            // 5. PaymentVoucherDetailEntity (depende de PaymentVoucherEntity)
            totalRegistros += copiarPaymentVoucherDetails(request, equivalencias, voucherEquiv, advertencias);

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

    // ================================================================
    // DUPLICATE helpers
    // ================================================================

    private int copiarTreasuries(CopyPhaseRequestDto request,
                                  List<CopyEquivalenciaDto> equivalencias) {
        List<TreasuryEntity> origenList = sourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());
        int count = 0;
        for (TreasuryEntity original : origenList) {
            TreasuryEntity nueva = new TreasuryEntity();
            nueva.setId(null);
            nueva.setAccountNumber(original.getAccountNumber());
            nueva.setBalance(original.getBalance());
            nueva.setCurrency(original.getCurrency());
            nueva.setAccountType(original.getAccountType());
            nueva.setStatus(original.isStatus());

            TreasuryEntity guardada = targetRepo.guardar(nueva);
            equivalencias.add(buildEquivalencia("treasury", original.getId(), guardada.getId()));
            count++;
        }
        return count;
    }

    private int copiarTransactions(CopyPhaseRequestDto request,
                                    List<CopyEquivalenciaDto> equivalencias,
                                    Map<Long, Long> transactionEquiv) {
        List<TransactionEntity> origenList = transactionSourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());
        int count = 0;
        for (TransactionEntity original : origenList) {
            TransactionEntity nueva = new TransactionEntity();
            nueva.setId(null);
            nueva.setDate(original.getDate());
            nueva.setDescription(original.getDescription());
            nueva.setDocumentType(original.getDocumentType());
            nueva.setDocumentId(original.getDocumentId());
            nueva.setUserId(original.getUserId());
            // createdAt → @PrePersist lo settea; splits → no se copian en cascada

            TransactionEntity guardada = transactionTargetRepo.guardar(nueva);
            transactionEquiv.put(original.getId(), guardada.getId());
            equivalencias.add(buildEquivalencia("transactions", original.getId(), guardada.getId()));
            count++;
        }
        return count;
    }

    private int copiarPaymentVouchers(CopyPhaseRequestDto request,
                                       List<CopyEquivalenciaDto> equivalencias,
                                       Map<Long, Long> voucherEquiv) {
        List<PaymentVoucherEntity> origenList = paymentVoucherSourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());
        int count = 0;
        for (PaymentVoucherEntity original : origenList) {
            PaymentVoucherEntity nueva = new PaymentVoucherEntity();
            nueva.setId(null);
            nueva.setDate(original.getDate());
            nueva.setPayeeId(original.getPayeeId());       // FK externa — sin remapeo
            nueva.setAmount(original.getAmount());
            nueva.setMethodId(original.getMethodId());     // FK externa — sin remapeo
            nueva.setBankAccountId(original.getBankAccountId()); // FK externa — sin remapeo
            nueva.setDescription(original.getDescription());
            nueva.setStatus(original.getStatus());
            // createdAt/updatedAt → @PrePersist/@PreUpdate los settean

            PaymentVoucherEntity guardada = paymentVoucherTargetRepo.guardar(nueva);
            voucherEquiv.put(original.getId(), guardada.getId());
            equivalencias.add(buildEquivalencia("payment_vouchers", original.getId(), guardada.getId()));
            count++;
        }
        return count;
    }

    private int copiarSplits(CopyPhaseRequestDto request,
                              List<CopyEquivalenciaDto> equivalencias,
                              Map<Long, Long> transactionEquiv,
                              List<String> advertencias) {
        List<SplitEntity> origenList = splitSourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());
        int count = 0;
        for (SplitEntity original : origenList) {
            Long nuevoTransactionId = transactionEquiv.get(original.getTransactionId());
            if (nuevoTransactionId == null) {
                advertencias.add("Split id=" + original.getId()
                        + " omitido: transactionId=" + original.getTransactionId()
                        + " no encontrado en equivalencias (posiblemente excluido por snapshotCorte)");
                continue;
            }

            SplitEntity nueva = new SplitEntity();
            nueva.setId(null);
            nueva.setAccountId(original.getAccountId()); // FK externa — sin remapeo
            nueva.setAmount(original.getAmount());
            nueva.setMemo(original.getMemo());

            // Establecer la relación @ManyToOne para que Hibernate resuelva transaction_id
            TransactionEntity txRef = new TransactionEntity();
            txRef.setId(nuevoTransactionId);
            nueva.setTransaction(txRef);

            SplitEntity guardada = splitTargetRepo.guardar(nueva);
            equivalencias.add(buildEquivalencia("splits", original.getId(), guardada.getId()));
            count++;
        }
        return count;
    }

    private int copiarPaymentVoucherDetails(CopyPhaseRequestDto request,
                                             List<CopyEquivalenciaDto> equivalencias,
                                             Map<Long, Long> voucherEquiv,
                                             List<String> advertencias) {
        List<PaymentVoucherDetailEntity> origenList = paymentVoucherDetailSourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());
        int count = 0;
        for (PaymentVoucherDetailEntity original : origenList) {
            Long nuevoVoucherId = voucherEquiv.get(original.getVoucherId());
            if (nuevoVoucherId == null) {
                advertencias.add("PaymentVoucherDetail id=" + original.getId()
                        + " omitido: voucherId=" + original.getVoucherId()
                        + " no encontrado en equivalencias (posiblemente excluido por snapshotCorte)");
                continue;
            }

            PaymentVoucherDetailEntity nueva = new PaymentVoucherDetailEntity();
            nueva.setId(null);
            nueva.setFactureId(original.getFactureId()); // FK externa — sin remapeo
            nueva.setAmount(original.getAmount());

            // Establecer la relación @ManyToOne para que Hibernate resuelva voucher_id
            PaymentVoucherEntity voucherRef = new PaymentVoucherEntity();
            voucherRef.setId(nuevoVoucherId);
            nueva.setPaymentVoucher(voucherRef);

            PaymentVoucherDetailEntity guardada = paymentVoucherDetailTargetRepo.guardar(nueva);
            equivalencias.add(buildEquivalencia("payment_voucher_details", original.getId(), guardada.getId()));
            count++;
        }
        return count;
    }

    // ================================================================
    // BACKUP: exportar datos del tenant origen
    // ================================================================

    private CopyPhaseResponseDto ejecutarExportacion(CopyPhaseRequestDto request) {
        log.info("Modo BACKUP treasury — exportando datos de entOrigen={}", request.getEntOrigen());

        Map<String, Object> datosExportados = new HashMap<>();
        int totalRegistros = 0;

        // Treasury
        List<TreasuryEntity> treasuries = sourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());
        List<Map<String, Object>> treasuryRows = new ArrayList<>();
        for (TreasuryEntity e : treasuries) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", e.getId());
            row.put("accountNumber", e.getAccountNumber());
            row.put("balance", e.getBalance());
            row.put("currency", e.getCurrency());
            row.put("accountType", e.getAccountType());
            row.put("status", e.isStatus());
            treasuryRows.add(row);
        }
        datosExportados.put("treasury", treasuryRows);
        totalRegistros += treasuryRows.size();

        // Transaction
        List<TransactionEntity> transactions = transactionSourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());
        List<Map<String, Object>> transactionRows = new ArrayList<>();
        for (TransactionEntity e : transactions) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", e.getId());
            row.put("date", e.getDate() != null ? e.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            row.put("description", e.getDescription());
            row.put("documentType", e.getDocumentType());
            row.put("documentId", e.getDocumentId());
            row.put("userId", e.getUserId());
            transactionRows.add(row);
        }
        datosExportados.put("transactions", transactionRows);
        totalRegistros += transactionRows.size();

        // PaymentVoucher
        List<PaymentVoucherEntity> vouchers = paymentVoucherSourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());
        List<Map<String, Object>> voucherRows = new ArrayList<>();
        for (PaymentVoucherEntity e : vouchers) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", e.getId());
            row.put("date", e.getDate() != null ? e.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            row.put("payeeId", e.getPayeeId());
            row.put("amount", e.getAmount());
            row.put("methodId", e.getMethodId());
            row.put("bankAccountId", e.getBankAccountId());
            row.put("description", e.getDescription());
            row.put("status", e.getStatus());
            voucherRows.add(row);
        }
        datosExportados.put("payment_vouchers", voucherRows);
        totalRegistros += voucherRows.size();

        // Split
        List<SplitEntity> splits = splitSourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());
        List<Map<String, Object>> splitRows = new ArrayList<>();
        for (SplitEntity e : splits) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", e.getId());
            row.put("transactionId", e.getTransactionId());
            row.put("accountId", e.getAccountId());
            row.put("amount", e.getAmount());
            row.put("memo", e.getMemo());
            splitRows.add(row);
        }
        datosExportados.put("splits", splitRows);
        totalRegistros += splitRows.size();

        // PaymentVoucherDetail
        List<PaymentVoucherDetailEntity> details = paymentVoucherDetailSourceRepo.findByEntOrigenBeforeSnapshot(
                request.getEntOrigen(), request.getSnapshotCorte());
        List<Map<String, Object>> detailRows = new ArrayList<>();
        for (PaymentVoucherDetailEntity e : details) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", e.getId());
            row.put("voucherId", e.getVoucherId());
            row.put("factureId", e.getFactureId());
            row.put("amount", e.getAmount());
            detailRows.add(row);
        }
        datosExportados.put("payment_voucher_details", detailRows);
        totalRegistros += detailRows.size();

        return CopyPhaseResponseDto.builder()
                .estado("COMPLETADO")
                .registrosProcesados(totalRegistros)
                .equivalenciasGeneradas(Collections.emptyList())
                .mensaje("Modo BACKUP — " + totalRegistros + " registros treasury exportados")
                .advertencias(Collections.emptyList())
                .datosExportados(datosExportados)
                .build();
    }

    // ================================================================
    // RESTORE: importar datos serializados en el tenant destino
    // ================================================================

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

            // 1. Treasury
            totalRegistros += restaurarTreasuries(datos, equivalencias);

            // 2. Transactions
            Map<Long, Long> transactionEquiv = new HashMap<>();
            totalRegistros += restaurarTransactions(datos, equivalencias, transactionEquiv);

            // 3. PaymentVouchers
            Map<Long, Long> voucherEquiv = new HashMap<>();
            totalRegistros += restaurarPaymentVouchers(datos, equivalencias, voucherEquiv);

            // 4. Splits (requiere transactionEquiv)
            totalRegistros += restaurarSplits(datos, equivalencias, transactionEquiv, advertencias);

            // 5. PaymentVoucherDetails (requiere voucherEquiv)
            totalRegistros += restaurarPaymentVoucherDetails(datos, equivalencias, voucherEquiv, advertencias);

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

    // ================================================================
    // RESTORE helpers
    // ================================================================

    @SuppressWarnings("unchecked")
    private int restaurarTreasuries(Map<String, Object> datos,
                                     List<CopyEquivalenciaDto> equivalencias) {
        List<Map<String, Object>> registros = (List<Map<String, Object>>) datos.get("treasury");
        if (registros == null) return 0;
        int count = 0;
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
            equivalencias.add(buildEquivalencia("treasury", idOriginal, guardada.getId()));
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private int restaurarTransactions(Map<String, Object> datos,
                                       List<CopyEquivalenciaDto> equivalencias,
                                       Map<Long, Long> transactionEquiv) {
        List<Map<String, Object>> registros = (List<Map<String, Object>>) datos.get("transactions");
        if (registros == null) return 0;
        int count = 0;
        for (Map<String, Object> row : registros) {
            Long idOriginal = toLong(row.get("id"));
            TransactionEntity nueva = new TransactionEntity();
            nueva.setId(null);
            nueva.setDate(toLocalDateTime(row.get("date")));
            nueva.setDescription(toStr(row.get("description")));
            nueva.setDocumentType(toStr(row.get("documentType")));
            nueva.setDocumentId(toLong(row.get("documentId")));
            nueva.setUserId(toStr(row.get("userId")));
            TransactionEntity guardada = transactionTargetRepo.guardar(nueva);
            transactionEquiv.put(idOriginal, guardada.getId());
            equivalencias.add(buildEquivalencia("transactions", idOriginal, guardada.getId()));
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private int restaurarPaymentVouchers(Map<String, Object> datos,
                                          List<CopyEquivalenciaDto> equivalencias,
                                          Map<Long, Long> voucherEquiv) {
        List<Map<String, Object>> registros = (List<Map<String, Object>>) datos.get("payment_vouchers");
        if (registros == null) return 0;
        int count = 0;
        for (Map<String, Object> row : registros) {
            Long idOriginal = toLong(row.get("id"));
            PaymentVoucherEntity nueva = new PaymentVoucherEntity();
            nueva.setId(null);
            nueva.setDate(toLocalDateTime(row.get("date")));
            nueva.setPayeeId(toLong(row.get("payeeId")));
            nueva.setAmount(toBigDecimal(row.get("amount")));
            nueva.setMethodId(toLong(row.get("methodId")));
            nueva.setBankAccountId(toLong(row.get("bankAccountId")));
            nueva.setDescription(toStr(row.get("description")));
            nueva.setStatus(toStr(row.get("status")));
            PaymentVoucherEntity guardada = paymentVoucherTargetRepo.guardar(nueva);
            voucherEquiv.put(idOriginal, guardada.getId());
            equivalencias.add(buildEquivalencia("payment_vouchers", idOriginal, guardada.getId()));
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private int restaurarSplits(Map<String, Object> datos,
                                 List<CopyEquivalenciaDto> equivalencias,
                                 Map<Long, Long> transactionEquiv,
                                 List<String> advertencias) {
        List<Map<String, Object>> registros = (List<Map<String, Object>>) datos.get("splits");
        if (registros == null) return 0;
        int count = 0;
        for (Map<String, Object> row : registros) {
            Long idOriginal = toLong(row.get("id"));
            Long txIdOriginal = toLong(row.get("transactionId"));
            Long nuevoTxId = transactionEquiv.get(txIdOriginal);
            if (nuevoTxId == null) {
                advertencias.add("Split id=" + idOriginal
                        + " omitido en RESTORE: transactionId=" + txIdOriginal
                        + " no encontrado en equivalencias");
                continue;
            }
            SplitEntity nueva = new SplitEntity();
            nueva.setId(null);
            nueva.setAccountId(toLong(row.get("accountId")));
            nueva.setAmount(toBigDecimal(row.get("amount")));
            nueva.setMemo(toStr(row.get("memo")));
            TransactionEntity txRef = new TransactionEntity();
            txRef.setId(nuevoTxId);
            nueva.setTransaction(txRef);
            SplitEntity guardada = splitTargetRepo.guardar(nueva);
            equivalencias.add(buildEquivalencia("splits", idOriginal, guardada.getId()));
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private int restaurarPaymentVoucherDetails(Map<String, Object> datos,
                                                List<CopyEquivalenciaDto> equivalencias,
                                                Map<Long, Long> voucherEquiv,
                                                List<String> advertencias) {
        List<Map<String, Object>> registros = (List<Map<String, Object>>) datos.get("payment_voucher_details");
        if (registros == null) return 0;
        int count = 0;
        for (Map<String, Object> row : registros) {
            Long idOriginal = toLong(row.get("id"));
            Long voucherIdOriginal = toLong(row.get("voucherId"));
            Long nuevoVoucherId = voucherEquiv.get(voucherIdOriginal);
            if (nuevoVoucherId == null) {
                advertencias.add("PaymentVoucherDetail id=" + idOriginal
                        + " omitido en RESTORE: voucherId=" + voucherIdOriginal
                        + " no encontrado en equivalencias");
                continue;
            }
            PaymentVoucherDetailEntity nueva = new PaymentVoucherDetailEntity();
            nueva.setId(null);
            nueva.setFactureId(toLong(row.get("factureId")));
            nueva.setAmount(toBigDecimal(row.get("amount")));
            PaymentVoucherEntity voucherRef = new PaymentVoucherEntity();
            voucherRef.setId(nuevoVoucherId);
            nueva.setPaymentVoucher(voucherRef);
            PaymentVoucherDetailEntity guardada = paymentVoucherDetailTargetRepo.guardar(nueva);
            equivalencias.add(buildEquivalencia("payment_voucher_details", idOriginal, guardada.getId()));
            count++;
        }
        return count;
    }

    // ================================================================
    // Helpers de conversión de tipos (JSON deserializado como Object)
    // ================================================================

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

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }

    private LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime ldt) return ldt;
        try { return LocalDateTime.parse(v.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
        catch (Exception e) { return null; }
    }

    private CopyEquivalenciaDto buildEquivalencia(String tabla, Long idViejo, Long idNuevo) {
        return CopyEquivalenciaDto.builder()
                .modulo(MODULO)
                .tabla(tabla)
                .idViejo(String.valueOf(idViejo))
                .idNuevo(String.valueOf(idNuevo))
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
