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
    }
}
