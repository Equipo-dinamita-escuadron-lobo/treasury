package com.treasury.infrastructure.adapters.input.transaction;

import com.treasury.application.input.IPayableWriteOffCommandUseCase;
import com.treasury.application.input.IPayableWriteOffQueryUseCase;
import com.treasury.application.output.IExecutionContextPort;
import com.treasury.application.output.IPayableWriteOffPersistencePort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.application.output.ITreasuryEventPublisher;
import com.treasury.application.service.PayableWriteOffService;
import com.treasury.domain.model.PayableWriteOff;
import com.treasury.domain.model.command.TreasuryCommands.AccountingResult;
import com.treasury.domain.model.command.TreasuryCommands.WriteOff;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionalPayableWriteOffUseCase
        implements IPayableWriteOffCommandUseCase, IPayableWriteOffQueryUseCase {
    private final PayableWriteOffService delegate;

    public TransactionalPayableWriteOffUseCase(IPayableWriteOffPersistencePort writeOffs,
            ISupplierInvoiceProviderPort invoices, ITreasuryEventPublisher events,
            ITreasuryAuditPersistencePort audit, IExecutionContextPort context) {
        this.delegate = new PayableWriteOffService(writeOffs, invoices, events, audit, context);
    }

    @Override @Transactional public PayableWriteOff create(WriteOff command) { return delegate.create(command); }
    @Override @Transactional public PayableWriteOff confirm(Long id) { return delegate.confirm(id); }
    @Override @Transactional public PayableWriteOff voidWriteOff(Long id) { return delegate.voidWriteOff(id); }
    @Override @Transactional public void applyAccountingResult(AccountingResult result) { delegate.applyAccountingResult(result); }
    @Override @Transactional(readOnly = true) public PayableWriteOff find(Long id) { return delegate.find(id); }
    @Override @Transactional(readOnly = true) public List<PayableWriteOff> list(String enterpriseId) { return delegate.list(enterpriseId); }
}
