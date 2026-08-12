package com.treasury.application.service;

import com.treasury.application.input.IInvoiceSynchronizationUseCase;
import com.treasury.domain.model.command.TreasuryCommands.PurchaseInvoiceEvent;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.SupplierInvoiceReplica;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

@RequiredArgsConstructor
public class InvoiceReplicaService implements IInvoiceSynchronizationUseCase {
    private final ISupplierInvoiceProviderPort invoices;
    private final ITreasuryAuditPersistencePort audit;

    @Override
    public void synchronize(PurchaseInvoiceEvent event) {
        if (audit.wasProcessed(event.eventId())) return;
        SupplierInvoiceReplica invoice = invoices.findBySource(event.invoiceId(), event.enterpriseId())
                .orElseGet(SupplierInvoiceReplica::new);
        boolean newInvoice = invoice.getId() == null;
        if (newInvoice) {
            invoice.setSourceInvoiceId(event.invoiceId()); invoice.setOriginalDueDate(event.dueDate());
            invoice.setDueDate(event.dueDate()); invoice.setReservedAmount(BigDecimal.ZERO);
            invoice.setPaidAmount(event.paidAmount() == null ? BigDecimal.ZERO : event.paidAmount());
            invoice.setPendingAmount(event.pendingAmount());
        } else {
            BigDecimal adjustedPending = invoice.getPendingAmount().add(event.originalAmount().subtract(invoice.getOriginalAmount()));
            if (adjustedPending.compareTo(invoice.getReservedAmount()) < 0)
                throw new TreasuryException(TreasuryException.Type.CONFLICT, "La actualización deja la obligación por debajo del saldo reservado");
            invoice.setPendingAmount(adjustedPending);
            if (!invoice.isDueDateOverridden()) invoice.setDueDate(event.dueDate());
        }
        invoice.setReference(event.reference()); invoice.setEnterpriseId(event.enterpriseId());
        invoice.setSupplierId(event.supplierId()); invoice.setOriginalAmount(event.originalAmount());
        invoice.setIssueDate(event.issueDate());
        invoice.setPayableAccountId(event.payableAccountId()); invoice.setPayableAccountCode(event.payableAccountCode());
        invoice.setActive(event.active() && !event.eventType().endsWith("VOIDED"));
        invoice.setLastEventId(event.eventId()); invoice.setTenantId(event.tenantId());
        invoices.save(invoice); audit.markProcessed(event.eventId(), event.tenantId());
    }
}
