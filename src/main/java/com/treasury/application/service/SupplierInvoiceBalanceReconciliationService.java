package com.treasury.application.service;

import com.treasury.application.output.IAccountCodeResolverPort;
import com.treasury.application.output.IPayableWriteOffPersistencePort;
import com.treasury.application.output.IPaymentVoucherQueryPersistencePort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.PayableWriteOff;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.command.TreasuryCommands.VoucherFilter;
import com.treasury.domain.service.SupplierInvoiceBalanceCalculator;
import com.treasury.domain.service.SupplierInvoiceBalanceCalculator.BalanceSnapshot;
import java.util.List;

public class SupplierInvoiceBalanceReconciliationService {
    private static final int MAX_MOVEMENTS = 10_000;

    private final ISupplierInvoiceProviderPort invoices;
    private final IPaymentVoucherQueryPersistencePort vouchers;
    private final IPayableWriteOffPersistencePort writeOffs;
    private final IAccountCodeResolverPort accountCodes;

    public SupplierInvoiceBalanceReconciliationService(ISupplierInvoiceProviderPort invoices,
            IPaymentVoucherQueryPersistencePort voucherQueries,
            IPayableWriteOffPersistencePort writeOffs) {
        this(invoices, voucherQueries, writeOffs, null);
    }

    public SupplierInvoiceBalanceReconciliationService(ISupplierInvoiceProviderPort invoices,
            IPaymentVoucherQueryPersistencePort voucherQueries,
            IPayableWriteOffPersistencePort writeOffs,
            IAccountCodeResolverPort accountCodes) {
        this.invoices = invoices;
        this.vouchers = voucherQueries;
        this.writeOffs = writeOffs;
        this.accountCodes = accountCodes;
    }

    public SupplierInvoiceReplica reconcile(Long invoiceId, String enterpriseId) {
        SupplierInvoiceReplica invoice = invoices.findLocked(invoiceId, enterpriseId)
                .orElseThrow(() -> new TreasuryException(TreasuryException.Type.NOT_FOUND,
                        "Obligación no encontrada"));
        BalanceSnapshot snapshot = SupplierInvoiceBalanceCalculator.calculate(invoice,
                vouchersForInvoice(enterpriseId, invoiceId),
                writeOffsForInvoice(enterpriseId, invoiceId));
        invoice.setPaidAmount(snapshot.paidAmount());
        invoice.setPendingAmount(snapshot.pendingAmount());
        invoice.setReservedAmount(snapshot.reservedAmount());
        normalizePayableCode(invoice);
        return invoices.save(invoice);
    }

    public int reconcileSupplier(String enterpriseId, Long supplierId) {
        List<SupplierInvoiceReplica> obligations = invoices.findForStatement(enterpriseId, supplierId, null, null,
                null, null);
        for (SupplierInvoiceReplica obligation : obligations) {
            reconcile(obligation.getId(), enterpriseId);
        }
        return obligations.size();
    }

    private List<PaymentVoucher> vouchersForInvoice(String enterpriseId, Long invoiceId) {
        VoucherFilter filter = new VoucherFilter(enterpriseId, null, null, null, null, null, invoiceId, null, null,
                null, null, 0, MAX_MOVEMENTS, "id,asc");
        return vouchers.search(filter).content();
    }

    private List<PayableWriteOff> writeOffsForInvoice(String enterpriseId, Long invoiceId) {
        return writeOffs.findByEnterprise(enterpriseId).stream()
                .filter(writeOff -> writeOff.getDetails().stream()
                        .anyMatch(detail -> invoiceId.equals(detail.getInvoiceId())))
                .toList();
    }

    private void normalizePayableCode(SupplierInvoiceReplica invoice) {
        if (accountCodes == null) {
            return;
        }
        Long accountId = invoice.getPayableAccountId();
        if (accountId == null) {
            return;
        }
        String code = invoice.getPayableAccountCode();
        boolean looksLikeId = code == null || code.isBlank() || code.equals(String.valueOf(accountId));
        if (!looksLikeId) {
            return;
        }
        accountCodes.resolveCode(accountId, invoice.getEnterpriseId())
                .ifPresent(invoice::setPayableAccountCode);
    }
}
