package com.treasury.application.service;

import com.treasury.domain.model.command.TreasuryCommands.AccountingResult;
import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.application.output.*;
import com.treasury.domain.model.*;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentScheduleAccountingRejectionL02Test {
    @Mock IPaymentSchedulePersistencePort schedules;
    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock IPaymentVoucherCommandUseCase voucherCommands;
    @Mock IPaymentVoucherQueryUseCase voucherQueries;
    @Mock IPaymentVoucherQueryPersistencePort voucherQueryPersistence;
    @Mock IExecutionContextPort context;
    @Mock ITransactionRunnerPort transactions;
    @Mock ITimeProviderPort time;
    @Mock IPaymentMethodProviderPort paymentMethods;

    PaymentScheduleService service;

    @BeforeEach
    void setUp() {
        service = new PaymentScheduleService(
                schedules, invoices, voucherCommands, voucherQueries, voucherQueryPersistence,
                context, transactions, time, paymentMethods);
        lenient().when(context.tenantId()).thenReturn("tenant");
        lenient().doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(context).runAsTenant(any(), any());
        lenient().doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(transactions).run(any());
        lenient().when(schedules.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(schedules.findWaitingAccounting(any())).thenReturn(java.util.List.of());
    }

    @Test
    void rejectedVoucherAccountingResultMarksWaitingScheduleAsFailed() {
        PaymentSchedule schedule = waitingSchedule(9L);

        when(schedules.findByVoucherId(9L)).thenReturn(Optional.of(schedule));

        service.applyAccountingResult(new AccountingResult(
                "result-schedule", "PAYMENT_VOUCHER", 9L, false, null,
                "Método de pago inactivo o sin equivalencia contable", "tenant"));

        assertThat(schedule.getStatus()).isEqualTo(PaymentScheduleStatus.FAILED);
        assertThat(schedule.getFailureReason()).contains("equivalencia contable");
    }

    @Test
    void reconcileWaitingAccountingUsesLinkedFailedVoucher() {
        PaymentSchedule schedule = waitingSchedule(11L);
        PaymentVoucher failedVoucher = failedVoucher(11L);

        when(schedules.findWaitingAccounting(any())).thenReturn(
                java.util.List.of(new DuePaymentSchedule(3L, "tenant")));
        when(schedules.findLocked(3L)).thenReturn(Optional.of(schedule));
        when(voucherQueryPersistence.findById(11L)).thenReturn(Optional.of(failedVoucher));

        service.recoverAbandoned(java.time.Instant.now());

        assertThat(schedule.getStatus()).isEqualTo(PaymentScheduleStatus.FAILED);
    }

    private PaymentSchedule waitingSchedule(Long voucherId) {
        PaymentSchedule schedule = new PaymentSchedule();
        schedule.setId(3L);
        schedule.setEnterpriseId("ent");
        schedule.setStatus(PaymentScheduleStatus.WAITING_ACCOUNTING);
        schedule.setVoucherId(voucherId);
        schedule.setTenantId("tenant");
        schedule.setDetails(new ArrayList<>());
        return schedule;
    }

    private PaymentVoucher failedVoucher(Long id) {
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setId(id);
        voucher.setEnterpriseId("ent");
        voucher.setStatus(PaymentVoucherStatus.FAILED);
        voucher.setFailureReason("Método de pago inactivo o sin equivalencia contable");
        voucher.setTenantId("tenant");
        return voucher;
    }
}
