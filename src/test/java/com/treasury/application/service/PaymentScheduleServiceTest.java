package com.treasury.application.service;

import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.application.output.IExecutionContextPort;
import com.treasury.application.output.IPaymentSchedulePersistencePort;
import com.treasury.application.output.IPaymentMethodProviderPort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITimeProviderPort;
import com.treasury.application.output.ITransactionRunnerPort;
import com.treasury.domain.model.DuePaymentSchedule;
import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.PaymentScheduleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentScheduleServiceTest {
    @Mock IPaymentSchedulePersistencePort schedules;
    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock IPaymentVoucherCommandUseCase voucherCommands;
    @Mock IPaymentVoucherQueryUseCase voucherQueries;
    @Mock IExecutionContextPort context;
    @Mock ITransactionRunnerPort transactions;
    @Mock ITimeProviderPort time;
    @Mock IPaymentMethodProviderPort paymentMethods;
    PaymentScheduleService service;

    @BeforeEach void setUp() {
        service = new PaymentScheduleService(schedules, invoices, voucherCommands,
                voucherQueries, context, transactions, time, paymentMethods);
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
}
