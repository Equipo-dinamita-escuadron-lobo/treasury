package com.treasury.infrastructure.adapters.input.transaction;

import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.application.output.IExecutionContextPort;
import com.treasury.application.output.IPayableWriteOffPersistencePort;
import com.treasury.application.output.IPaymentVoucherCommandPersistencePort;
import com.treasury.application.output.IPaymentVoucherQueryPersistencePort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.application.output.ITreasuryEventPublisher;
import com.treasury.application.output.IPaymentMethodProviderPort;
import com.treasury.application.service.PaymentVoucherService;
import com.treasury.application.service.SupplierInvoiceBalanceReconciliationService;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.command.TreasuryCommands.AccountingResult;
import com.treasury.domain.model.command.TreasuryCommands.PageResult;
import com.treasury.domain.model.command.TreasuryCommands.Voucher;
import com.treasury.domain.model.command.TreasuryCommands.VoucherFilter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionalPaymentVoucherUseCase
        implements IPaymentVoucherCommandUseCase, IPaymentVoucherQueryUseCase {
    private final PaymentVoucherService delegate;

    public TransactionalPaymentVoucherUseCase(IPaymentVoucherCommandPersistencePort voucherCommands,
            IPaymentVoucherQueryPersistencePort voucherQueries,
            ISupplierInvoiceProviderPort invoices, ITreasuryEventPublisher events,
            ITreasuryAuditPersistencePort audit, IExecutionContextPort context,
            IPaymentMethodProviderPort paymentMethods,
            IPayableWriteOffPersistencePort writeOffs) {
        SupplierInvoiceBalanceReconciliationService reconciliation =
                new SupplierInvoiceBalanceReconciliationService(invoices, voucherQueries, writeOffs);
        this.delegate = new PaymentVoucherService(voucherCommands, voucherQueries, invoices, events, audit, context,
                paymentMethods, reconciliation);
    }

    @Override @Transactional public PaymentVoucher create(Voucher command) { return delegate.create(command); }
    @Override @Transactional public PaymentVoucher update(Long id, Voucher command) { return delegate.update(id, command); }
    @Override @Transactional public void delete(Long id, String enterpriseId) { delegate.delete(id, enterpriseId); }
    @Override @Transactional public PaymentVoucher post(Long id, String enterpriseId, String key) { return delegate.post(id, enterpriseId, key); }
    @Override @Transactional public PaymentVoucher voidVoucher(Long id, String enterpriseId, String reason) { return delegate.voidVoucher(id, enterpriseId, reason); }
    @Override @Transactional public void applyAccountingResult(AccountingResult result) { delegate.applyAccountingResult(result); }
    @Override @Transactional(readOnly = true) public PaymentVoucher find(Long id, String enterpriseId) { return delegate.find(id, enterpriseId); }
    @Override @Transactional(readOnly = true) public PageResult<PaymentVoucher> search(VoucherFilter filter) { return delegate.search(filter); }
}
