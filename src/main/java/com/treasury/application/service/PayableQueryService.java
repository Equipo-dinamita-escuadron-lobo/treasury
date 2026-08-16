package com.treasury.application.service;

import com.treasury.application.input.IPayableCommandUseCase;
import com.treasury.application.input.IPayableQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.application.output.*;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PayableQueryService implements IPayableCommandUseCase, IPayableQueryUseCase {
    private final ISupplierInvoiceProviderPort invoices;
    private final IPaymentVoucherQueryPersistencePort vouchers;
    private final IPayableWriteOffPersistencePort writeOffs;
    private final ITreasuryAuditPersistencePort audit;
    private final IExecutionContextPort context;
    private final SupplierInvoiceBalanceReconciliationService reconciliation;
    private final IAccountCodeResolverPort accountCodes;

    public PayableQueryService(ISupplierInvoiceProviderPort invoices,
            IPaymentVoucherQueryPersistencePort voucherQueries,
            IPayableWriteOffPersistencePort writeOffs,
            ITreasuryAuditPersistencePort audit,
            IExecutionContextPort context) {
        this(invoices, voucherQueries, writeOffs, audit, context, null, null);
    }

    public PayableQueryService(ISupplierInvoiceProviderPort invoices,
            IPaymentVoucherQueryPersistencePort voucherQueries,
            IPayableWriteOffPersistencePort writeOffs,
            ITreasuryAuditPersistencePort audit,
            IExecutionContextPort context,
            SupplierInvoiceBalanceReconciliationService reconciliation,
            IAccountCodeResolverPort accountCodes) {
        this.invoices = invoices;
        this.vouchers = voucherQueries;
        this.writeOffs = writeOffs;
        this.audit = audit;
        this.context = context;
        this.reconciliation = reconciliation != null
                ? reconciliation
                : new SupplierInvoiceBalanceReconciliationService(invoices, voucherQueries, writeOffs, accountCodes);
        this.accountCodes = accountCodes;
    }

    @Override public List<SupplierInvoiceReplica> pending(String enterpriseId, Long supplierId) { return invoices.findPending(enterpriseId, supplierId); }
    @Override public SupplierInvoiceReplica find(Long id, String enterpriseId) {
        return invoices.findById(id)
                .filter(invoice -> enterpriseId.equals(invoice.getEnterpriseId()))
                .orElseThrow(() -> new TreasuryException(TreasuryException.Type.NOT_FOUND,
                        "ObligaciÃ³n no encontrada"));
    }
    @Override public SupplierInvoiceReplica updateDueDate(Long id, String enterpriseId, DueDate command) {
        SupplierInvoiceReplica invoice=locked(id,enterpriseId); LocalDate previous=invoice.getDueDate(); invoice.changeDueDate(command.dueDate());
        audit.recordDueDate(id,previous,command.dueDate(),command.reason(),context.username(),context.tenantId()); return invoices.save(invoice);
    }

    @Override
    public SupplierInvoiceReplica reconcileBalance(Long id, String enterpriseId) {
        return reconciliation.reconcile(id, enterpriseId);
    }

    @Override
    public int reconcileSupplierBalances(String enterpriseId, Long supplierId) {
        return reconciliation.reconcileSupplier(enterpriseId, supplierId);
    }
    @Override public SupplierStatement statement(String enterpriseId, Long supplierId, LocalDate from, LocalDate to, String invoiceReference, Boolean active) {
        List<SupplierInvoiceReplica> throughEnd=invoices.findForStatement(enterpriseId,supplierId,null,to,invoiceReference,active);
        List<SupplierInvoiceReplica> periodInvoices=throughEnd.stream()
                .filter(invoice->from==null||!invoice.getIssueDate().isBefore(from))
                .toList();
        List<SupplierInvoiceReplica> openingInvoices=throughEnd.stream()
                .filter(invoice->from!=null&&invoice.getIssueDate().isBefore(from))
                .toList();
        Set<Long> openingInvoiceIds=openingInvoices.stream()
                .map(SupplierInvoiceReplica::getId)
                .collect(Collectors.toSet());
        BigDecimal openingBalance=sum(openingInvoices,SupplierInvoiceReplica::getPendingAmount);
        BigDecimal invoiced=sum(periodInvoices,SupplierInvoiceReplica::getOriginalAmount);
        BigDecimal closingBalance=sum(throughEnd,SupplierInvoiceReplica::getPendingAmount);
        Long invoiceId=periodInvoices.size()==1?periodInvoices.get(0).getId():null;
        VoucherFilter filter=new VoucherFilter(enterpriseId,null,PaymentVoucherStatus.POSTED,from,to,supplierId,invoiceId,null,null,null,null,0,10000,"id,desc");
        List<PaymentVoucher> voucherList=vouchers.search(filter).content();
        BigDecimal periodPayments=voucherList.stream()
                .flatMap(voucher->voucher.getDetails().stream())
                .filter(detail->supplierId==null||supplierId.equals(detail.getSupplierId()))
                .map(PaymentVoucherDetail::getAmountPaid)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        List<PayableWriteOff> enterpriseWriteOffs=writeOffs.findByEnterprise(enterpriseId).stream()
                .filter(writeOff->writeOff.getDetails().stream().anyMatch(detail->supplierId==null||supplierId.equals(detail.getSupplierId())))
                .toList();
        List<PayableWriteOff> postedWriteOffList=enterpriseWriteOffs.stream()
                .filter(writeOff->writeOff.getStatus()==WriteOffStatus.POSTED)
                .filter(writeOff->inPeriod(writeOff.getCreatedAt(),from,to))
                .toList();
        List<PayableWriteOff> traceWriteOffList=enterpriseWriteOffs.stream()
                .filter(this::isStatementTraceWriteOff)
                .filter(writeOff->isWriteOffTraceableInPeriod(writeOff,from,to))
                .toList();
        BigDecimal writeOffTotal=postedWriteOffList.stream()
                .flatMap(writeOff->writeOff.getDetails().stream())
                .filter(detail->supplierId==null||supplierId.equals(detail.getSupplierId()))
                .map(PayableWriteOffDetail::getAmount)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        if(from!=null){
            for(PaymentVoucher voucher:voucherList){
                for(PaymentVoucherDetail detail:voucher.getDetails()){
                    if(detail.getInvoiceId()!=null&&openingInvoiceIds.contains(detail.getInvoiceId())
                            &&(supplierId==null||supplierId.equals(detail.getSupplierId()))){
                        openingBalance=openingBalance.add(detail.getAmountPaid());
                    }
                }
            }
            for(PayableWriteOff writeOff:postedWriteOffList){
                for(PayableWriteOffDetail detail:writeOff.getDetails()){
                    if(detail.getInvoiceId()!=null&&openingInvoiceIds.contains(detail.getInvoiceId())
                            &&(supplierId==null||supplierId.equals(detail.getSupplierId()))){
                        openingBalance=openingBalance.add(detail.getAmount());
                    }
                }
            }
        }
        return new SupplierStatement(supplierId,invoiced,periodPayments,closingBalance,periodInvoices,voucherList,openingBalance,writeOffTotal,traceWriteOffList);
    }
    @Override public List<AgingLine> aging(String enterpriseId, LocalDate cutoff, Long supplierId, String accountCode, String document) {
        return pending(enterpriseId,supplierId).stream()
                .filter(i->accountCode==null||accountCode.equals(displayAccountCode(i)))
                .filter(i->document==null||document.isBlank()||i.getReference().toLowerCase().contains(document.toLowerCase()))
                .map(i->{
            long days=Math.max(0,ChronoUnit.DAYS.between(i.getDueDate(),cutoff));BigDecimal value=i.getPendingAmount(),zero=BigDecimal.ZERO;
            return new AgingLine(i.getSupplierId(),i.getId(),i.getReference(),displayAccountCode(i),i.getDueDate(),days,days==0?value:zero,days>=1&&days<=30?value:zero,days>=31&&days<=60?value:zero,days>=61&&days<=90?value:zero,days>90?value:zero);
        }).toList();
    }

    private String displayAccountCode(SupplierInvoiceReplica invoice) {
        String code = invoice.getPayableAccountCode();
        Long accountId = invoice.getPayableAccountId();
        if (accountCodes == null || accountId == null) {
            return code;
        }
        boolean looksLikeId = code == null || code.isBlank() || code.equals(String.valueOf(accountId));
        if (!looksLikeId) {
            return code;
        }
        return accountCodes.resolveCode(accountId, invoice.getEnterpriseId()).orElse(code);
    }
    private boolean inPeriod(Instant instant,LocalDate from,LocalDate to){
        if(instant==null)return false;
        LocalDate date=instant.atZone(ZoneId.systemDefault()).toLocalDate();
        if(from!=null&&date.isBefore(from))return false;
        if(to!=null&&date.isAfter(to))return false;
        return true;
    }
    private boolean isStatementTraceWriteOff(PayableWriteOff writeOff) {
        return writeOff.getStatus() == WriteOffStatus.POSTED || writeOff.getStatus() == WriteOffStatus.VOIDED;
    }
    private boolean isWriteOffTraceableInPeriod(PayableWriteOff writeOff, LocalDate from, LocalDate to) {
        if (writeOff.getStatus() == WriteOffStatus.POSTED) {
            return inPeriod(writeOff.getCreatedAt(), from, to);
        }
        if (writeOff.getStatus() == WriteOffStatus.VOIDED) {
            return inPeriod(writeOff.getCreatedAt(), from, to) || inPeriod(writeOff.getUpdatedAt(), from, to);
        }
        return false;
    }
    private BigDecimal sum(List<SupplierInvoiceReplica> values,java.util.function.Function<SupplierInvoiceReplica,BigDecimal> mapper){return values.stream().map(mapper).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private SupplierInvoiceReplica locked(Long id,String enterpriseId){return invoices.findLocked(id,enterpriseId).orElseThrow(()->new TreasuryException(TreasuryException.Type.NOT_FOUND,"Obligación no encontrada"));}
}
