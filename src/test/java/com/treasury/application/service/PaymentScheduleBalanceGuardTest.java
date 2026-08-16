package com.treasury.application.service;

import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.application.output.IExecutionContextPort;
import com.treasury.application.output.IPaymentSchedulePersistencePort;
import com.treasury.application.output.IPaymentMethodProviderPort;
import com.treasury.application.output.IPaymentVoucherCommandPersistencePort;
import com.treasury.application.output.IPaymentVoucherQueryPersistencePort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITimeProviderPort;
import com.treasury.application.output.ITransactionRunnerPort;
import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.application.output.ITreasuryEventPublisher;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.PaymentScheduleDetail;
import com.treasury.domain.model.PaymentScheduleStatus;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.PaymentVoucherDetail;
import com.treasury.domain.model.PaymentVoucherStatus;
import com.treasury.domain.model.PayableWriteOff;
import com.treasury.domain.model.PayableWriteOffDetail;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.WriteOffStatus;
import com.treasury.domain.model.command.TreasuryCommands.Detail;
import com.treasury.domain.model.command.TreasuryCommands.Schedule;
import com.treasury.domain.model.command.TreasuryCommands.ScheduleFilter;
import com.treasury.domain.model.command.TreasuryCommands.Voucher;
import com.treasury.domain.model.command.TreasuryCommands.WriteOff;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentScheduleBalanceGuardTest {
    @Mock IPaymentSchedulePersistencePort schedules;
    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock IPaymentVoucherCommandPersistencePort voucherCommands;
    @Mock IPaymentVoucherQueryPersistencePort voucherQueries;
    @Mock ITreasuryEventPublisher events;
    @Mock ITreasuryAuditPersistencePort audit;
    @Mock IExecutionContextPort context;
    @Mock IPaymentMethodProviderPort paymentMethods;
    @Mock IPaymentVoucherCommandUseCase voucherCommandUseCase;
    @Mock IPaymentVoucherQueryUseCase voucherQueryUseCase;
    @Mock IPaymentVoucherQueryPersistencePort voucherQueryPersistence;
    @Mock ITransactionRunnerPort transactions;
    @Mock ITimeProviderPort time;

    PaymentScheduleBalanceGuard guard;
    PaymentVoucherService voucherService;
    PayableWriteOffService writeOffService;
    PaymentScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        guard = new PaymentScheduleBalanceGuard(schedules);
        voucherService = new PaymentVoucherService(voucherCommands, voucherQueries, invoices, events, audit, context,
                paymentMethods, null, guard);
        writeOffService = new PayableWriteOffService(
                org.mockito.Mockito.mock(com.treasury.application.output.IPayableWriteOffPersistencePort.class),
                invoices, events, audit, context, guard);
        scheduleService = new PaymentScheduleService(schedules, invoices, voucherCommandUseCase, voucherQueryUseCase,
                voucherQueryPersistence, context, transactions, time, paymentMethods, guard);
        lenient().when(context.tenantId()).thenReturn("tenant");
        lenient().when(schedules.search(any(ScheduleFilter.class))).thenReturn(List.of());
        lenient().doNothing().when(paymentMethods).validateForPayment(anyLong(), any(), anyString());
        lenient().when(voucherCommands.save(any())).thenAnswer(invocation -> {
            PaymentVoucher voucher = invocation.getArgument(0);
            if (voucher.getId() == null) {
                voucher.setId(99L);
            }
            return voucher;
        });
        lenient().when(time.today()).thenReturn(LocalDate.of(2026, 8, 20));
    }

    @Test
    void rejectsManualPaymentWhenActiveScheduleExists() {
        PaymentSchedule schedule = schedule(1L, PaymentScheduleStatus.SCHEDULED, detail(11L, 10L, "100000"));
        when(schedules.search(any(ScheduleFilter.class))).thenReturn(List.of(schedule));

        assertThatThrownBy(() -> voucherService.create(voucherCommand(11L, 10L, "40000")))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining(PaymentScheduleBalanceGuard.ACTIVE_SCHEDULE_BLOCKS_BALANCE_CHANGE);

        verify(voucherCommands, never()).save(any());
    }

    @Test
    void rejectsWriteOffWhenActiveScheduleExists() {
        PaymentSchedule schedule = schedule(1L, PaymentScheduleStatus.SCHEDULED, detail(11L, 10L, "100000"));
        when(schedules.search(any(ScheduleFilter.class))).thenReturn(List.of(schedule));
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice(11L, 10L, "100000")));
        com.treasury.application.output.IPayableWriteOffPersistencePort writeOffs =
                org.mockito.Mockito.mock(com.treasury.application.output.IPayableWriteOffPersistencePort.class);
        when(writeOffs.findByEnterprise("ent")).thenReturn(List.of());
        writeOffService = new PayableWriteOffService(writeOffs, invoices, events, audit, context, guard);

        assertThatThrownBy(() -> writeOffService.create(writeOffCommand(11L, 10L, "40000")))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining(PaymentScheduleBalanceGuard.ACTIVE_SCHEDULE_BLOCKS_BALANCE_CHANGE);
    }

    @Test
    void rejectsVoidWriteOffWhenActiveScheduleExists() {
        PaymentSchedule schedule = schedule(1L, PaymentScheduleStatus.SCHEDULED, detail(11L, 10L, "60000"));
        when(schedules.search(any(ScheduleFilter.class))).thenReturn(List.of(schedule));
        com.treasury.application.output.IPayableWriteOffPersistencePort writeOffs =
                org.mockito.Mockito.mock(com.treasury.application.output.IPayableWriteOffPersistencePort.class);
        PayableWriteOff writeOff = postedWriteOff(11L, 10L, "40000");
        when(writeOffs.find(30L)).thenReturn(Optional.of(writeOff));
        writeOffService = new PayableWriteOffService(writeOffs, invoices, events, audit, context, guard);

        assertThatThrownBy(() -> writeOffService.voidWriteOff(30L))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining(PaymentScheduleBalanceGuard.ACTIVE_SCHEDULE_BLOCKS_BALANCE_CHANGE);
    }

    @Test
    void rejectsVoidPaymentWhenActiveScheduleExists() {
        PaymentSchedule schedule = schedule(1L, PaymentScheduleStatus.SCHEDULED, detail(11L, 10L, "70000"));
        when(schedules.search(any(ScheduleFilter.class))).thenReturn(List.of(schedule));
        PaymentVoucher voucher = postedVoucher(11L, 10L, "30000");
        when(voucherQueries.find(50L, "ent")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> voucherService.voidVoucher(50L, "ent", "motivo"))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining(PaymentScheduleBalanceGuard.ACTIVE_SCHEDULE_BLOCKS_BALANCE_CHANGE);
    }

    @Test
    void rejectsSecondScheduleForSameInvoice() {
        PaymentSchedule existing = schedule(1L, PaymentScheduleStatus.SCHEDULED, detail(11L, 10L, "50000"));
        when(schedules.search(any(ScheduleFilter.class))).thenReturn(List.of(existing));

        assertThatThrownBy(() -> scheduleService.create(scheduleCommand(11L, 10L, "50000")))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining(PaymentScheduleBalanceGuard.ACTIVE_SCHEDULE_BLOCKS_NEW_SCHEDULE);
    }

    @Test
    void allowsScheduleExecutionDespiteActiveBalanceLock() {
        PaymentSchedule schedule = schedule(5L, PaymentScheduleStatus.SCHEDULED, detail(11L, 10L, "100000"));
        SupplierInvoiceReplica invoice = invoice(11L, 10L, "100000");
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setId(88L);
        voucher.setEnterpriseId("ent");
        when(schedules.findLocked(5L)).thenReturn(Optional.of(schedule));
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice));
        when(voucherCommandUseCase.create(any(Voucher.class))).thenReturn(voucher);
        when(schedules.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentSchedule result = scheduleService.execute(5L);

        assertThat(result.getStatus()).isEqualTo(PaymentScheduleStatus.WAITING_ACCOUNTING);
        verify(voucherCommandUseCase).create(any(Voucher.class));
        verify(voucherCommandUseCase).post(eq(88L), eq("ent"), anyString());
    }

    @Test
    void executeRejectsObsoleteScheduleAmountAgainstCurrentPending() {
        PaymentSchedule schedule = schedule(5L, PaymentScheduleStatus.SCHEDULED, detail(11L, 10L, "100000"));
        SupplierInvoiceReplica invoice = invoice(11L, 10L, "60000");
        when(schedules.findLocked(5L)).thenReturn(Optional.of(schedule));
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice));
        when(schedules.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentSchedule result = scheduleService.execute(5L);

        assertThat(result.getStatus()).isEqualTo(PaymentScheduleStatus.FAILED);
        assertThat(result.getFailureReason()).contains("supera el saldo disponible");
        verify(voucherCommandUseCase, never()).create(any());
    }

    @Test
    void blocksOnlyInvoicesIncludedInGroupedSchedule() {
        PaymentSchedule schedule = schedule(3L, PaymentScheduleStatus.SCHEDULED,
                detail(11L, 10L, "100000"),
                detail(12L, 20L, "200000"));
        when(schedules.search(any(ScheduleFilter.class))).thenReturn(List.of(schedule));
        when(invoices.findById(13L)).thenReturn(Optional.of(invoice(13L, 30L, "50000")));

        assertThatThrownBy(() -> voucherService.create(voucherCommand(11L, 10L, "10000")))
                .isInstanceOf(TreasuryException.class);
        voucherService.create(voucherCommand(13L, 30L, "10000"));
        verify(voucherCommands).save(any());
    }

    @Test
    void rejectsVoidWhenAnyInvoiceInVoucherHasActiveSchedule() {
        PaymentSchedule schedule = schedule(1L, PaymentScheduleStatus.SCHEDULED, detail(11L, 10L, "50000"));
        when(schedules.search(any(ScheduleFilter.class))).thenReturn(List.of(schedule));
        PaymentVoucher voucher = multiInvoiceVoucher(
                detailVoucher(11L, 10L, "20000"),
                detailVoucher(12L, 20L, "30000"));
        when(voucherQueries.find(60L, "ent")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> voucherService.voidVoucher(60L, "ent", "motivo"))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining(PaymentScheduleBalanceGuard.ACTIVE_SCHEDULE_BLOCKS_BALANCE_CHANGE);
    }

    private static PaymentSchedule schedule(Long id, PaymentScheduleStatus status, PaymentScheduleDetail... details) {
        PaymentSchedule schedule = new PaymentSchedule();
        schedule.setId(id);
        schedule.setEnterpriseId("ent");
        schedule.setExecutionDate(LocalDate.of(2026, 8, 20));
        schedule.setPaymentMethodId(1L);
        schedule.setStatus(status);
        schedule.setTenantId("tenant");
        schedule.setDetails(new ArrayList<>(List.of(details)));
        return schedule;
    }

    private static PaymentScheduleDetail detail(Long invoiceId, Long supplierId, String amount) {
        PaymentScheduleDetail detail = new PaymentScheduleDetail();
        detail.setInvoiceId(invoiceId);
        detail.setSupplierId(supplierId);
        detail.setAmount(bd(amount));
        detail.setTenantId("tenant");
        return detail;
    }

    private static SupplierInvoiceReplica invoice(Long id, Long supplierId, String pending) {
        SupplierInvoiceReplica invoice = new SupplierInvoiceReplica();
        invoice.setId(id);
        invoice.setSourceInvoiceId(id);
        invoice.setEnterpriseId("ent");
        invoice.setSupplierId(supplierId);
        invoice.setReference("F-" + id);
        invoice.setPendingAmount(bd(pending));
        invoice.setOriginalAmount(bd(pending));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setReservedAmount(BigDecimal.ZERO);
        invoice.setActive(true);
        invoice.setTenantId("tenant");
        return invoice;
    }

    private static Voucher voucherCommand(Long invoiceId, Long supplierId, String amount) {
        return new Voucher("ent", LocalDate.now(), 1L, null, "test",
                List.of(new Detail(supplierId, invoiceId, bd(amount))));
    }

    private static WriteOff writeOffCommand(Long invoiceId, Long supplierId, String amount) {
        return new WriteOff("ent", "motivo", 99L, "4999",
                List.of(new Detail(supplierId, invoiceId, bd(amount))));
    }

    private static Schedule scheduleCommand(Long invoiceId, Long supplierId, String amount) {
        return new Schedule("ent", LocalDate.of(2026, 8, 25), 1L, null, "obs",
                List.of(new Detail(supplierId, invoiceId, bd(amount))));
    }

    private static PayableWriteOff postedWriteOff(Long invoiceId, Long supplierId, String amount) {
        PayableWriteOffDetail detail = new PayableWriteOffDetail();
        detail.setInvoiceId(invoiceId);
        detail.setSupplierId(supplierId);
        detail.setAmount(bd(amount));
        PayableWriteOff writeOff = new PayableWriteOff();
        writeOff.setId(30L);
        writeOff.setEnterpriseId("ent");
        writeOff.setStatus(WriteOffStatus.POSTED);
        writeOff.setDetails(List.of(detail));
        return writeOff;
    }

    private static PaymentVoucher postedVoucher(Long invoiceId, Long supplierId, String amount) {
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setId(50L);
        voucher.setEnterpriseId("ent");
        voucher.setStatus(PaymentVoucherStatus.POSTED);
        voucher.setDetails(List.of(detailVoucher(invoiceId, supplierId, amount)));
        return voucher;
    }

    private static PaymentVoucherDetail detailVoucher(Long invoiceId, Long supplierId, String amount) {
        PaymentVoucherDetail detail = new PaymentVoucherDetail();
        detail.setInvoiceId(invoiceId);
        detail.setSupplierId(supplierId);
        detail.setAmountPaid(bd(amount));
        return detail;
    }

    private static PaymentVoucher multiInvoiceVoucher(PaymentVoucherDetail... details) {
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setId(60L);
        voucher.setEnterpriseId("ent");
        voucher.setStatus(PaymentVoucherStatus.POSTED);
        voucher.setDetails(List.of(details));
        return voucher;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
