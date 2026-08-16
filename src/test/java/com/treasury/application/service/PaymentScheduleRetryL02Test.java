package com.treasury.application.service;

import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.application.output.IExecutionContextPort;
import com.treasury.application.output.IPaymentMethodProviderPort;
import com.treasury.application.output.IPaymentSchedulePersistencePort;
import com.treasury.application.output.IPaymentVoucherCommandPersistencePort;
import com.treasury.application.output.IPaymentVoucherQueryPersistencePort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITimeProviderPort;
import com.treasury.application.output.ITransactionRunnerPort;
import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.application.output.ITreasuryEventPublisher;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.PaymentMethodData;
import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.PaymentScheduleDetail;
import com.treasury.domain.model.PaymentScheduleStatus;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.PaymentVoucherDetail;
import com.treasury.domain.model.PaymentVoucherStatus;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.TreasuryEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L-02 / B-26: retry of a FAILED payment schedule must re-enqueue the accounting
 * event so the schedule can leave WAITING_ACCOUNTING and reach a terminal state.
 */
@ExtendWith(MockitoExtension.class)
class PaymentScheduleRetryL02Test {
    @Mock IPaymentSchedulePersistencePort schedules;
    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock IPaymentVoucherCommandPersistencePort voucherCommands;
    @Mock IPaymentVoucherQueryPersistencePort voucherQueries;
    @Mock ITreasuryEventPublisher events;
    @Mock ITreasuryAuditPersistencePort audit;
    @Mock IExecutionContextPort context;
    @Mock ITransactionRunnerPort transactions;
    @Mock ITimeProviderPort time;
    @Mock IPaymentMethodProviderPort paymentMethods;

    PaymentVoucherService voucherService;
    PaymentScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        voucherService = new PaymentVoucherService(
                voucherCommands, voucherQueries, invoices, events, audit, context, paymentMethods);
        scheduleService = new PaymentScheduleService(
                schedules, invoices, voucherService, voucherService, voucherQueries,
                context, transactions, time, paymentMethods);
        lenient().when(context.tenantId()).thenReturn("tenant");
        lenient().doNothing().when(paymentMethods).validateForPayment(anyLong(), any(), anyString());
        lenient().when(paymentMethods.findActive(anyLong(), anyString()))
                .thenReturn(Optional.of(new PaymentMethodData(1L, false, 1105L)));
        lenient().when(voucherCommands.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(schedules.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void retryOfFailedScheduleReEnqueuesAccountingEventAndWaitsForAck() {
        PaymentVoucher failedVoucher = failedVoucherLinkedToSchedule();
        PaymentSchedule failedSchedule = failedSchedule(failedVoucher.getId());
        SupplierInvoiceReplica invoice = invoiceAvailable();

        when(schedules.findLocked(7L)).thenReturn(Optional.of(failedSchedule));
        when(voucherQueries.find(10L, "ent")).thenReturn(Optional.of(failedVoucher));
        when(voucherQueries.findByIdempotencyKey(anyString(), eq("ent"))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return key.equals(failedVoucher.getIdempotencyKey()) ? Optional.of(failedVoucher) : Optional.empty();
        });
        when(invoices.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoices.findLocked(1L, "ent")).thenReturn(Optional.of(invoice));

        PaymentSchedule result = scheduleService.execute(7L);

        assertThat(result.getStatus()).isEqualTo(PaymentScheduleStatus.WAITING_ACCOUNTING);
        assertThat(failedVoucher.getStatus()).isEqualTo(PaymentVoucherStatus.POSTING);
        assertThat(failedVoucher.getIdempotencyKey()).isEqualTo("schedule-7-1");
        ArgumentCaptor<TreasuryEvent> eventCaptor = ArgumentCaptor.forClass(TreasuryEvent.class);
        verify(events, times(1)).enqueue(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo("PAYMENT_VOUCHER_CREATED");
        assertThat(eventCaptor.getValue().aggregateId()).isEqualTo(10L);
    }

    @Test
    void retryFailsWhenPaymentMethodBecameInactiveBeforePost() {
        PaymentVoucher failedVoucher = failedVoucherLinkedToSchedule();
        PaymentSchedule failedSchedule = failedSchedule(failedVoucher.getId());

        when(schedules.findLocked(7L)).thenReturn(Optional.of(failedSchedule));
        when(voucherQueries.find(10L, "ent")).thenReturn(Optional.of(failedVoucher));
        when(voucherQueries.findByIdempotencyKey(anyString(), eq("ent"))).thenReturn(Optional.empty());
        when(invoices.findById(1L)).thenReturn(Optional.of(invoiceAvailable()));
        doThrow(new TreasuryException(TreasuryException.Type.BAD_REQUEST, "Metodo de pago inactivo o inexistente"))
                .when(paymentMethods).validateForPayment(1L, null, "ent");

        PaymentSchedule result = scheduleService.execute(7L);

        assertThat(result.getStatus()).isEqualTo(PaymentScheduleStatus.FAILED);
        assertThat(result.getFailureReason()).contains("inactivo");
        assertThat(failedVoucher.getStatus()).isEqualTo(PaymentVoucherStatus.FAILED);
        verify(events, never()).enqueue(any());
        verify(invoices, never()).findLocked(anyLong(), anyString());
    }

    @Test
    void retryFailsWhenAccountingAccountBecameInactiveBeforePost() {
        PaymentVoucher failedVoucher = failedVoucherLinkedToSchedule();
        PaymentSchedule failedSchedule = failedSchedule(failedVoucher.getId());

        when(schedules.findLocked(7L)).thenReturn(Optional.of(failedSchedule));
        when(voucherQueries.find(10L, "ent")).thenReturn(Optional.of(failedVoucher));
        when(voucherQueries.findByIdempotencyKey(anyString(), eq("ent"))).thenReturn(Optional.empty());
        when(invoices.findById(1L)).thenReturn(Optional.of(invoiceAvailable()));
        doThrow(new TreasuryException(TreasuryException.Type.BAD_REQUEST, "La cuenta contable del metodo de pago esta inactiva o no pertenece a la empresa"))
                .when(paymentMethods).validateForPayment(1L, null, "ent");

        PaymentSchedule result = scheduleService.execute(7L);

        assertThat(result.getStatus()).isEqualTo(PaymentScheduleStatus.FAILED);
        assertThat(result.getFailureReason()).contains("cuenta contable del metodo de pago");
        assertThat(failedVoucher.getStatus()).isEqualTo(PaymentVoucherStatus.FAILED);
        verify(events, never()).enqueue(any());
        verify(invoices, never()).findLocked(anyLong(), anyString());
    }

    private PaymentSchedule failedSchedule(Long voucherId) {
        PaymentScheduleDetail detail = new PaymentScheduleDetail();
        detail.setSupplierId(10L);
        detail.setInvoiceId(1L);
        detail.setAmount(new BigDecimal("20"));
        detail.setTenantId("tenant");
        PaymentSchedule schedule = new PaymentSchedule();
        schedule.setId(7L);
        schedule.setEnterpriseId("ent");
        schedule.setExecutionDate(LocalDate.of(2026, 8, 10));
        schedule.setPaymentMethodId(1L);
        schedule.setObservations("retry-l02");
        schedule.setStatus(PaymentScheduleStatus.FAILED);
        schedule.setVoucherId(voucherId);
        schedule.setRetryCount(1);
        schedule.setFailureReason("Asiento rechazado");
        schedule.setTenantId("tenant");
        schedule.setDetails(new ArrayList<>(List.of(detail)));
        return schedule;
    }

    private PaymentVoucher failedVoucherLinkedToSchedule() {
        PaymentVoucherDetail detail = new PaymentVoucherDetail();
        detail.setSupplierId(10L);
        detail.setInvoiceId(1L);
        detail.setInvoiceReference("F-1");
        detail.setPayableAccountId(11L);
        detail.setPayableAccountCode("2205");
        detail.setPreviousBalance(new BigDecimal("100"));
        detail.setAmountPaid(new BigDecimal("20"));
        detail.setRemainingBalance(new BigDecimal("80"));
        detail.setTenantId("tenant");
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setId(10L);
        voucher.setVoucherNumber("CE-L02");
        voucher.setEnterpriseId("ent");
        voucher.setIssueDate(LocalDate.of(2026, 8, 10));
        voucher.setStatus(PaymentVoucherStatus.FAILED);
        voucher.setPaymentMethodId(1L);
        voucher.setTotal(new BigDecimal("20"));
        voucher.setIdempotencyKey("schedule-7");
        voucher.setFailureReason("Asiento rechazado");
        voucher.setTenantId("tenant");
        voucher.setDetails(List.of(detail));
        return voucher;
    }

    private SupplierInvoiceReplica invoiceAvailable() {
        SupplierInvoiceReplica invoice = new SupplierInvoiceReplica();
        invoice.setId(1L);
        invoice.setSourceInvoiceId(1L);
        invoice.setEnterpriseId("ent");
        invoice.setSupplierId(10L);
        invoice.setReference("F-1");
        invoice.setPendingAmount(new BigDecimal("100"));
        invoice.setOriginalAmount(new BigDecimal("100"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setReservedAmount(BigDecimal.ZERO);
        invoice.setPayableAccountId(11L);
        invoice.setPayableAccountCode("2205");
        invoice.setActive(true);
        invoice.setTenantId("tenant");
        return invoice;
    }
}
