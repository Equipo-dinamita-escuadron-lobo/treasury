package com.treasury.application.service;

import com.treasury.application.output.IExecutionContextPort;
import com.treasury.application.output.IPayableWriteOffPersistencePort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.application.output.ITreasuryEventPublisher;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.PayableWriteOff;
import com.treasury.domain.model.PayableWriteOffDetail;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.WriteOffStatus;
import com.treasury.domain.model.command.TreasuryCommands.AccountingResult;
import com.treasury.domain.model.command.TreasuryCommands.Detail;
import com.treasury.domain.model.command.TreasuryCommands.WriteOff;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayableWriteOffServiceTest {
    @Mock IPayableWriteOffPersistencePort writeOffs;
    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock ITreasuryEventPublisher events;
    @Mock ITreasuryAuditPersistencePort audit;
    @Mock IExecutionContextPort context;
    PayableWriteOffService service;

    @BeforeEach
    void setUp() {
        service = new PayableWriteOffService(writeOffs, invoices, events, audit, context);
        lenient().when(context.tenantId()).thenReturn("tenant");
        lenient().when(writeOffs.save(any())).thenAnswer(invocation -> {
            PayableWriteOff value = invocation.getArgument(0);
            if (value.getId() == null) {
                value.setId(9L);
            }
            return value;
        });
    }

    @Test
    void createsPartialWriteOffWithPayableAccountFromObligation() {
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice(11L, 7L, "500", "0", 2205L, "220501")));
        PayableWriteOff result = service.create(command(new Detail(7L, 11L, bd("200"))));

        assertThat(result.getTotal()).isEqualByComparingTo("200");
        PayableWriteOffDetail detail = result.getDetails().get(0);
        assertThat(detail.getInvoiceId()).isEqualTo(11L);
        assertThat(detail.getSupplierId()).isEqualTo(7L);
        assertThat(detail.getPayableAccountId()).isEqualTo(2205L);
        assertThat(detail.getPayableAccountCode()).isEqualTo("220501");
        assertThat(result.getStatus()).isEqualTo(WriteOffStatus.DRAFT);
    }

    @Test
    void createsTotalWriteOff() {
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice(11L, 7L, "500", "0", 2205L, "220501")));
        PayableWriteOff result = service.create(command(new Detail(7L, 11L, bd("500"))));
        assertThat(result.getTotal()).isEqualByComparingTo("500");
    }

    @Test
    void rejectsWriteOffWhenObligationNotFound() {
        when(invoices.findById(11L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(command(new Detail(7L, 11L, bd("10")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("Obligación no encontrada");
    }

    @Test
    void rejectsWriteOffForObligationFromAnotherEnterprise() {
        SupplierInvoiceReplica otherEnterprise = invoice(11L, 7L, "500", "0", 2205L, "220501");
        otherEnterprise.setEnterpriseId("other-ent");
        when(invoices.findById(11L)).thenReturn(Optional.of(otherEnterprise));
        assertThatThrownBy(() -> service.create(command(new Detail(7L, 11L, bd("10")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("Obligación no encontrada");
    }

    @Test
    void rejectsWriteOffWhenNoAvailableBalance() {
        SupplierInvoiceReplica reserved = invoice(11L, 7L, "500", "500", 2205L, "220501");
        when(invoices.findById(11L)).thenReturn(Optional.of(reserved));
        assertThatThrownBy(() -> service.create(command(new Detail(7L, 11L, bd("10")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void rejectsWriteOffAmountGreaterThanAvailable() {
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice(11L, 7L, "500", "0", 2205L, "220501")));
        assertThatThrownBy(() -> service.create(command(new Detail(7L, 11L, bd("600")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void rejectsWriteOffWithWrongSupplier() {
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice(11L, 7L, "500", "0", 2205L, "220501")));
        assertThatThrownBy(() -> service.create(command(new Detail(99L, 11L, bd("10")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void confirmAcceptedWriteOffReducesPendingBalance() {
        SupplierInvoiceReplica invoice = invoice(11L, 7L, "500", "200", 2205L, "220501");
        PayableWriteOff writeOff = draftWriteOff(invoice, bd("200"));
        writeOff.setStatus(WriteOffStatus.POSTING);
        when(writeOffs.find(9L)).thenReturn(Optional.of(writeOff));
        when(invoices.findLocked(11L, "ent")).thenReturn(Optional.of(invoice));
        when(audit.wasProcessed("result-ok")).thenReturn(false);

        service.applyAccountingResult(new AccountingResult(
                "result-ok", "PAYABLE_WRITEOFF", 9L, true, 88L, null, "tenant"));

        assertThat(invoice.getPendingAmount()).isEqualByComparingTo("300");
        assertThat(invoice.getReservedAmount()).isZero();
        assertThat(writeOff.getStatus()).isEqualTo(WriteOffStatus.POSTED);
    }

    @Test
    void confirmRejectedWriteOffReleasesReservationWithoutChangingPending() {
        SupplierInvoiceReplica invoice = invoice(11L, 7L, "500", "200", 2205L, "220501");
        PayableWriteOff writeOff = draftWriteOff(invoice, bd("200"));
        writeOff.setStatus(WriteOffStatus.POSTING);
        when(writeOffs.find(9L)).thenReturn(Optional.of(writeOff));
        when(invoices.findLocked(11L, "ent")).thenReturn(Optional.of(invoice));
        when(audit.wasProcessed("result-fail")).thenReturn(false);

        service.applyAccountingResult(new AccountingResult(
                "result-fail", "PAYABLE_WRITEOFF", 9L, false, null, "rechazo", "tenant"));

        assertThat(invoice.getPendingAmount()).isEqualByComparingTo("500");
        assertThat(invoice.getReservedAmount()).isZero();
        assertThat(writeOff.getStatus()).isEqualTo(WriteOffStatus.FAILED);
    }

    @Test
    void confirmPublishesAccountingEventAndReservesBalance() {
        SupplierInvoiceReplica invoice = invoice(11L, 7L, "500", "0", 2205L, "220501");
        PayableWriteOff writeOff = draftWriteOff(invoice, bd("200"));
        when(writeOffs.find(9L)).thenReturn(Optional.of(writeOff));
        when(invoices.findLocked(11L, "ent")).thenReturn(Optional.of(invoice));

        PayableWriteOff result = service.confirm(9L);

        assertThat(result.getStatus()).isEqualTo(WriteOffStatus.POSTING);
        assertThat(invoice.getReservedAmount()).isEqualByComparingTo("200");
        verify(events).enqueue(any());
    }

    @Test
    void voidAccountingAckIsAuditedWithoutApplyingBalancesAgain() {
        PayableWriteOff writeOff = new PayableWriteOff();
        writeOff.setId(7L);
        writeOff.setStatus(WriteOffStatus.VOIDED);
        when(audit.wasProcessed("result-void")).thenReturn(false);
        when(writeOffs.find(7L)).thenReturn(Optional.of(writeOff));

        service.applyAccountingResult(new AccountingResult(
                "result-void", "PAYABLE_WRITEOFF", 7L, true, 19L, null, "tenant"));

        verify(audit).markProcessed("result-void", "tenant");
        verifyNoInteractions(invoices);
        verify(writeOffs, never()).save(any());
    }

    private WriteOff command(Detail... details) {
        return new WriteOff("ent", "condonacion", 4295L, "429501", List.of(details));
    }

    private PayableWriteOff draftWriteOff(SupplierInvoiceReplica invoice, BigDecimal amount) {
        PayableWriteOff writeOff = new PayableWriteOff();
        writeOff.setId(9L);
        writeOff.setEnterpriseId("ent");
        writeOff.setReason("condonacion");
        writeOff.setCounterpartAccountId(4295L);
        writeOff.setCounterpartAccountCode("429501");
        writeOff.setStatus(WriteOffStatus.DRAFT);
        writeOff.setTenantId("tenant");
        PayableWriteOffDetail detail = new PayableWriteOffDetail();
        detail.setInvoiceId(invoice.getId());
        detail.setSupplierId(invoice.getSupplierId());
        detail.setPayableAccountId(invoice.getPayableAccountId());
        detail.setPayableAccountCode(invoice.getPayableAccountCode());
        detail.setAmount(amount);
        detail.setTenantId("tenant");
        writeOff.setDetails(List.of(detail));
        writeOff.setTotal(amount);
        return writeOff;
    }

    private SupplierInvoiceReplica invoice(
            long id, long supplier, String pending, String reserved, long payableId, String payableCode) {
        SupplierInvoiceReplica invoice = new SupplierInvoiceReplica();
        invoice.setId(id);
        invoice.setSourceInvoiceId(id);
        invoice.setEnterpriseId("ent");
        invoice.setSupplierId(supplier);
        invoice.setReference("FC-" + id);
        invoice.setPendingAmount(bd(pending));
        invoice.setOriginalAmount(bd(pending));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setReservedAmount(bd(reserved));
        invoice.setPayableAccountId(payableId);
        invoice.setPayableAccountCode(payableCode);
        invoice.setActive(true);
        invoice.setTenantId("tenant");
        return invoice;
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
