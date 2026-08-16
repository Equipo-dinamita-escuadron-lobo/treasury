package com.treasury.application.service;

import com.treasury.application.input.IInvoiceSynchronizationUseCase;
import com.treasury.application.output.IAccountCodeResolverPort;
import com.treasury.domain.model.AccountCatalogueAccountSnapshot;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.command.TreasuryCommands.PurchaseInvoiceEvent;
import java.math.BigDecimal;
import java.util.Optional;

public class InvoiceReplicaService implements IInvoiceSynchronizationUseCase {
    private final ISupplierInvoiceProviderPort invoices;
    private final ITreasuryAuditPersistencePort audit;
    private final IAccountCodeResolverPort accountCodes;

    public InvoiceReplicaService(ISupplierInvoiceProviderPort invoices, ITreasuryAuditPersistencePort audit) {
        this(invoices, audit, null);
    }

    public InvoiceReplicaService(ISupplierInvoiceProviderPort invoices,
            ITreasuryAuditPersistencePort audit,
            IAccountCodeResolverPort accountCodes) {
        this.invoices = invoices;
        this.audit = audit;
        this.accountCodes = accountCodes;
    }

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
            validatePayableAccount(event);
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
        invoice.setPayableAccountId(event.payableAccountId());
        invoice.setPayableAccountCode(normalizePayableCode(event));
        invoice.setActive(event.active() && !event.eventType().endsWith("VOIDED"));
        invoice.setLastEventId(event.eventId()); invoice.setTenantId(event.tenantId());
        invoices.save(invoice); audit.markProcessed(event.eventId(), event.tenantId());
    }

    /** Si llega el id numérico como código, intenta normalizar contra el catálogo. */
    private String normalizePayableCode(PurchaseInvoiceEvent event) {
        String code = event.payableAccountCode();
        Long accountId = event.payableAccountId();
        if (accountId == null) return code;
        boolean looksLikeId = code == null || code.isBlank() || code.equals(String.valueOf(accountId));
        if (!looksLikeId || accountCodes == null) return code;
        return accountCodes.resolveCode(accountId, event.enterpriseId()).orElse(code);
    }

    private void validatePayableAccount(PurchaseInvoiceEvent event) {
        if (accountCodes == null || event.payableAccountId() == null) {
            return;
        }
        Optional<AccountCatalogueAccountSnapshot> account = accountCodes.resolveAccount(event.payableAccountId(),
                event.enterpriseId());
        if (account.isEmpty()) {
            return;
        }
        if (!account.get().isPayableLiability()) {
            throw new TreasuryException(TreasuryException.Type.CONFLICT,
                    "La cuenta por pagar configurada no es una obligación válida del catálogo");
        }
    }
}
