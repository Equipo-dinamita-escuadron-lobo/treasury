package com.treasury.application.service;

import com.treasury.application.input.IPayableCommandUseCase;
import com.treasury.application.input.IPayableQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.application.output.*;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.*;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
public class PayableQueryService implements IPayableCommandUseCase, IPayableQueryUseCase {
    private final ISupplierInvoiceProviderPort invoices;
    private final IPaymentVoucherQueryPersistencePort vouchers;
    private final ITreasuryAuditPersistencePort audit;
    private final IExecutionContextPort context;

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
    @Override public SupplierStatement statement(String enterpriseId, Long supplierId, LocalDate from, LocalDate to, String invoiceReference, Boolean active) {
        List<SupplierInvoiceReplica> list=invoices.findForStatement(enterpriseId,supplierId,from,to,invoiceReference,active);
        BigDecimal invoiced=sum(list,SupplierInvoiceReplica::getOriginalAmount),paid=sum(list,SupplierInvoiceReplica::getPaidAmount),pending=sum(list,SupplierInvoiceReplica::getPendingAmount);
        Long invoiceId=list.size()==1?list.get(0).getId():null;VoucherFilter filter=new VoucherFilter(enterpriseId,null,null,from,to,supplierId,invoiceId,null,null,null,null,0,10000,"id,desc");
        return new SupplierStatement(supplierId,invoiced,paid,pending,list,vouchers.search(filter).content());
    }
    @Override public List<AgingLine> aging(String enterpriseId, LocalDate cutoff, Long supplierId, String accountCode, String document) {
        return pending(enterpriseId,supplierId).stream().filter(i->accountCode==null||i.getPayableAccountCode().equals(accountCode)).filter(i->document==null||document.isBlank()||i.getReference().toLowerCase().contains(document.toLowerCase())).map(i->{
            long days=Math.max(0,ChronoUnit.DAYS.between(i.getDueDate(),cutoff));BigDecimal value=i.getPendingAmount(),zero=BigDecimal.ZERO;
            return new AgingLine(i.getSupplierId(),i.getId(),i.getReference(),i.getPayableAccountCode(),i.getDueDate(),days,days==0?value:zero,days>=1&&days<=30?value:zero,days>=31&&days<=60?value:zero,days>=61&&days<=90?value:zero,days>90?value:zero);
        }).toList();
    }
    private BigDecimal sum(List<SupplierInvoiceReplica> values,java.util.function.Function<SupplierInvoiceReplica,BigDecimal> mapper){return values.stream().map(mapper).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private SupplierInvoiceReplica locked(Long id,String enterpriseId){return invoices.findLocked(id,enterpriseId).orElseThrow(()->new TreasuryException(TreasuryException.Type.NOT_FOUND,"Obligación no encontrada"));}
}
