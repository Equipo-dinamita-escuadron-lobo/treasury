package com.treasury.infrastructure.adapters.input.transaction;

import com.treasury.application.input.IInvoiceSynchronizationUseCase;
import com.treasury.application.output.IAccountCodeResolverPort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.application.service.InvoiceReplicaService;
import com.treasury.domain.model.command.TreasuryCommands.PurchaseInvoiceEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionalInvoiceSynchronizationUseCase implements IInvoiceSynchronizationUseCase {
    private final InvoiceReplicaService delegate;

    public TransactionalInvoiceSynchronizationUseCase(ISupplierInvoiceProviderPort invoices,
            ITreasuryAuditPersistencePort audit,
            IAccountCodeResolverPort accountCodes) {
        this.delegate = new InvoiceReplicaService(invoices, audit, accountCodes);
    }

    @Override
    @Transactional
    public void synchronize(PurchaseInvoiceEvent event) {
        delegate.synchronize(event);
    }
}
