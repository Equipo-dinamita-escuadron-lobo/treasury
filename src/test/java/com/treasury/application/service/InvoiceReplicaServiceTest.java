package com.treasury.application.service;

import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.command.TreasuryCommands.PurchaseInvoiceEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceReplicaServiceTest {
    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock ITreasuryAuditPersistencePort audit;

    @Test void createsFullyPendingActiveInvoiceWithEventBalances() {
        when(invoices.findBySource(9002L, "ent")).thenReturn(Optional.empty());
        var service = new InvoiceReplicaService(invoices, audit);

        service.synchronize(event("event-1", 9002L, "100000", "0", "100000", true));

        ArgumentCaptor<SupplierInvoiceReplica> saved = ArgumentCaptor.forClass(SupplierInvoiceReplica.class);
        verify(invoices).save(saved.capture());
        assertThat(saved.getValue().getOriginalAmount()).isEqualByComparingTo("100000");
        assertThat(saved.getValue().getPaidAmount()).isEqualByComparingTo("0");
        assertThat(saved.getValue().getPendingAmount()).isEqualByComparingTo("100000");
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(saved.getValue().getTenantId()).isEqualTo("tenant");
        verify(audit).markProcessed("event-1", "tenant");
    }

    @Test void createsPartiallyPaidInvoiceWithRemainingBalance() {
        when(invoices.findBySource(9003L, "ent")).thenReturn(Optional.empty());
        var service = new InvoiceReplicaService(invoices, audit);

        service.synchronize(event("event-2", 9003L, "100000", "40000", "60000", true));

        ArgumentCaptor<SupplierInvoiceReplica> saved = ArgumentCaptor.forClass(SupplierInvoiceReplica.class);
        verify(invoices).save(saved.capture());
        assertThat(saved.getValue().getPaidAmount()).isEqualByComparingTo("40000");
        assertThat(saved.getValue().getPendingAmount()).isEqualByComparingTo("60000");
    }

    @Test void createsFullyPaidReplicaWithZeroPendingBalance() {
        when(invoices.findBySource(9004L, "ent")).thenReturn(Optional.empty());
        var service = new InvoiceReplicaService(invoices, audit);

        service.synchronize(event("event-3", 9004L, "100000", "100000", "0", true));

        ArgumentCaptor<SupplierInvoiceReplica> saved = ArgumentCaptor.forClass(SupplierInvoiceReplica.class);
        verify(invoices).save(saved.capture());
        assertThat(saved.getValue().getPendingAmount()).isEqualByComparingTo("0");
        assertThat(saved.getValue().isActive()).isTrue();
    }

    @Test void ignoresRepeatedEventIdWithoutSavingAgain() {
        when(audit.wasProcessed("event-repeated")).thenReturn(true);
        var service = new InvoiceReplicaService(invoices, audit);

        service.synchronize(event("event-repeated", 9005L, "100000", "0", "100000", true));

        verifyNoInteractions(invoices);
        verify(audit, never()).markProcessed(anyString(), anyString());
    }

    @Test void replayPreservesTreasuryPaymentsAndAdjustsOnlyOriginalDelta() {
        SupplierInvoiceReplica current = new SupplierInvoiceReplica();
        current.setId(1L); current.setSourceInvoiceId(9001L); current.setEnterpriseId("ent");
        current.setOriginalAmount(new BigDecimal("100")); current.setPaidAmount(new BigDecimal("40"));
        current.setPendingAmount(new BigDecimal("60")); current.setReservedAmount(BigDecimal.ZERO);
        when(invoices.findBySource(9001L, "ent")).thenReturn(Optional.of(current));
        var service = new InvoiceReplicaService(invoices, audit);

        service.synchronize(new PurchaseInvoiceEvent("event-2", "PURCHASE_INVOICE_UPDATED",
                9001L, "F-9001", "ent", 77L, new BigDecimal("120"), BigDecimal.ZERO,
                new BigDecimal("120"), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 30),
                2205L, "2205", true, "tenant"));

        ArgumentCaptor<SupplierInvoiceReplica> saved = ArgumentCaptor.forClass(SupplierInvoiceReplica.class);
        verify(invoices).save(saved.capture());
        assertThat(saved.getValue().getPaidAmount()).isEqualByComparingTo("40");
        assertThat(saved.getValue().getPendingAmount()).isEqualByComparingTo("80");
        assertThat(saved.getValue().getId()).isEqualTo(1L);
        verify(audit).markProcessed("event-2", "tenant");
    }

    private PurchaseInvoiceEvent event(String eventId, Long invoiceId, String original,
            String paid, String pending, boolean active) {
        return new PurchaseInvoiceEvent(eventId, "PURCHASE_INVOICE_CREATED",
                invoiceId, "F-" + invoiceId, "ent", 77L, new BigDecimal(original),
                new BigDecimal(paid), new BigDecimal(pending), LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 9, 16), 2205L, "2205", active, "tenant");
    }
}
