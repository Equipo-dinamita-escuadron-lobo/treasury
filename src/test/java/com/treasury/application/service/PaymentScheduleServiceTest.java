package com.treasury.application.service;

import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.application.output.IExecutionContextPort;
import com.treasury.application.output.IPaymentSchedulePersistencePort;
import com.treasury.application.output.IPaymentMethodProviderPort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITimeProviderPort;
import com.treasury.application.output.IPaymentVoucherQueryPersistencePort;
import com.treasury.application.output.ITransactionRunnerPort;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.DuePaymentSchedule;
import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.PaymentScheduleStatus;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.command.TreasuryCommands.Detail;
import com.treasury.domain.model.command.TreasuryCommands.Schedule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentScheduleServiceTest {
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

    @BeforeEach void setUp() {
        service = new PaymentScheduleService(schedules, invoices, voucherCommands,
                voucherQueries, voucherQueryPersistence, context, transactions, time, paymentMethods);
    }

    @Test void schedulerInstallsTenantBeforeOpeningTransaction() {
        LocalDate date = LocalDate.of(2026, 8, 10);
        PaymentSchedule alreadyExecuted = new PaymentSchedule();
        alreadyExecuted.setId(3L);
        alreadyExecuted.setStatus(PaymentScheduleStatus.EXECUTED);
        when(schedules.findDue(date)).thenReturn(List.of(new DuePaymentSchedule(3L, "tenant-a")));
        when(schedules.findLocked(3L)).thenReturn(Optional.of(alreadyExecuted));
        doAnswer(invocation -> { ((Runnable) invocation.getArgument(1)).run(); return null; })
                .when(context).runAsTenant(eq("tenant-a"), any(Runnable.class));
        doAnswer(invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; })
                .when(transactions).run(any(Runnable.class));

        service.executeDue(date);

        InOrder order = inOrder(context, transactions);
        order.verify(context).runAsTenant(eq("tenant-a"), any(Runnable.class));
        order.verify(transactions).run(any(Runnable.class));
        verify(schedules).findLocked(3L);
    }

    @Test void rejectsTodayAsExecutionDate() {
        LocalDate today = LocalDate.of(2026, 8, 16);
        when(time.today()).thenReturn(today);
        Schedule command = scheduleCommand(today);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining(PaymentScheduleService.FUTURE_EXECUTION_DATE_REQUIRED);

        verify(schedules, never()).save(any());
        verifyNoInteractions(invoices);
    }

    @Test void rejectsPastExecutionDate() {
        LocalDate today = LocalDate.of(2026, 8, 16);
        when(time.today()).thenReturn(today);
        Schedule command = scheduleCommand(today.minusDays(1));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining(PaymentScheduleService.FUTURE_EXECUTION_DATE_REQUIRED);

        verify(schedules, never()).save(any());
        verifyNoInteractions(invoices);
    }

    @Test void allowsFutureExecutionDate() {
        LocalDate today = LocalDate.of(2026, 8, 16);
        when(time.today()).thenReturn(today);
        when(context.tenantId()).thenReturn("tenant");
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice()));
        when(schedules.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Schedule command = scheduleCommand(today.plusDays(1));

        service.create(command);

        verify(schedules).save(any());
    }

    @Test void acceptsAmountEqualToAvailableBalance() {
        LocalDate today = LocalDate.of(2026, 8, 16);
        SupplierInvoiceReplica invoice = invoice();
        invoice.setPendingAmount(new BigDecimal("100000"));
        when(time.today()).thenReturn(today);
        when(context.tenantId()).thenReturn("tenant");
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice));
        when(schedules.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentSchedule result = service.create(scheduleCommand(today.plusDays(1), "100000"));

        assertThat(result.getDetails()).singleElement()
                .extracting(detail -> detail.getAmount())
                .isEqualTo(new BigDecimal("100000"));
    }

    @Test void rejectsAmountGreaterThanAvailableBalance() {
        LocalDate today = LocalDate.of(2026, 8, 16);
        SupplierInvoiceReplica invoice = invoice();
        invoice.setPendingAmount(new BigDecimal("100000"));
        invoice.setReference("FC-100");
        when(time.today()).thenReturn(today);
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.create(scheduleCommand(today.plusDays(1), "100001")))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("supera el saldo disponible de FC-100");

        verify(schedules, never()).save(any());
    }

    @Test void rejectsZeroAmount() {
        LocalDate today = LocalDate.of(2026, 8, 16);
        when(time.today()).thenReturn(today);

        assertThatThrownBy(() -> service.create(scheduleCommand(today.plusDays(1), "0")))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("mayor que cero");

        verify(schedules, never()).save(any());
    }

    private static Schedule scheduleCommand(LocalDate executionDate) {
        return scheduleCommand(executionDate, "100");
    }

    private static Schedule scheduleCommand(LocalDate executionDate, String amount) {
        return new Schedule("ent", executionDate, 1L, null, "obs",
                List.of(new Detail(10L, 11L, new BigDecimal(amount))));
    }

    private static SupplierInvoiceReplica invoice() {
        SupplierInvoiceReplica invoice = new SupplierInvoiceReplica();
        invoice.setId(11L);
        invoice.setEnterpriseId("ent");
        invoice.setSupplierId(10L);
        invoice.setPendingAmount(new BigDecimal("100"));
        invoice.setReservedAmount(BigDecimal.ZERO);
        invoice.setActive(true);
        return invoice;
    }
}
