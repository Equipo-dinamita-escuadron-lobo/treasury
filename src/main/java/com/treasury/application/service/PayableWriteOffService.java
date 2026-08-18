package com.treasury.application.service;

import com.treasury.application.input.IPayableWriteOffCommandUseCase;
import com.treasury.application.input.IPayableWriteOffQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.application.output.*;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.*;
import java.math.BigDecimal;
import java.util.*;

public class PayableWriteOffService implements IPayableWriteOffCommandUseCase, IPayableWriteOffQueryUseCase {
    private final IPayableWriteOffPersistencePort writeOffs;
    private final ISupplierInvoiceProviderPort invoices;
    private final ITreasuryEventPublisher events;
    private final ITreasuryAuditPersistencePort audit;
    private final IExecutionContextPort context;
    private final PaymentScheduleBalanceGuard scheduleBalanceGuard;

    public PayableWriteOffService(IPayableWriteOffPersistencePort writeOffs, ISupplierInvoiceProviderPort invoices,
            ITreasuryEventPublisher events, ITreasuryAuditPersistencePort audit, IExecutionContextPort context) {
        this(writeOffs, invoices, events, audit, context, null);
    }

    public PayableWriteOffService(IPayableWriteOffPersistencePort writeOffs, ISupplierInvoiceProviderPort invoices,
            ITreasuryEventPublisher events, ITreasuryAuditPersistencePort audit, IExecutionContextPort context,
            PaymentScheduleBalanceGuard scheduleBalanceGuard) {
        this.writeOffs = writeOffs;
        this.invoices = invoices;
        this.events = events;
        this.audit = audit;
        this.context = context;
        this.scheduleBalanceGuard = scheduleBalanceGuard;
    }

    @Override public PayableWriteOff create(WriteOff command){
        PayableWriteOff writeOff=new PayableWriteOff();writeOff.setEnterpriseId(command.enterpriseId());writeOff.setReason(command.reason());writeOff.setCounterpartAccountId(command.counterpartAccountId());writeOff.setCounterpartAccountCode(command.counterpartAccountCode());writeOff.setTenantId(context.tenantId());
        List<PayableWriteOffDetail> details=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;Set<Long> unique=new HashSet<>();
        for(Detail requested:command.details()){
            if(requested.amount()==null||requested.amount().signum()<=0)throw new TreasuryException(TreasuryException.Type.BAD_REQUEST,"El valor debe ser mayor que cero");
            if(!unique.add(requested.invoiceId()))conflict("Una obligación no puede repetirse en la baja");
            SupplierInvoiceReplica invoice=invoices.findById(requested.invoiceId()).filter(i->i.getEnterpriseId().equals(command.enterpriseId())&&i.isActive()).orElseThrow(()->notFound("Obligación no encontrada"));
            if(hasActiveWriteOffForInvoice(command.enterpriseId(),requested.invoiceId()))conflict("La obligación ya tiene una baja de CxP pendiente de contabilización.");
            if(!invoice.getSupplierId().equals(requested.supplierId())||requested.amount().compareTo(invoice.available())>0)conflict("Detalle de baja inválido");
            PayableWriteOffDetail detail=new PayableWriteOffDetail();detail.setSupplierId(invoice.getSupplierId());detail.setInvoiceId(invoice.getId());detail.setPayableAccountId(invoice.getPayableAccountId());detail.setPayableAccountCode(invoice.getPayableAccountCode());detail.setAmount(requested.amount());detail.setTenantId(context.tenantId());details.add(detail);total=total.add(requested.amount());
        }
        assertBalanceChangeAllowed(command.enterpriseId(), details.stream().map(PayableWriteOffDetail::getInvoiceId).toList());
        writeOff.setDetails(details);writeOff.setTotal(total);return writeOffs.save(writeOff);
    }
    @Override public PayableWriteOff confirm(Long id){PayableWriteOff writeOff=get(id);assertBalanceChangeAllowed(writeOff.getEnterpriseId(),writeOff.getDetails().stream().map(PayableWriteOffDetail::getInvoiceId).toList());writeOff.startPosting();for(PayableWriteOffDetail detail:writeOff.getDetails()){SupplierInvoiceReplica invoice=locked(detail.getInvoiceId(),writeOff.getEnterpriseId());detail.setPayableAccountId(invoice.getPayableAccountId());detail.setPayableAccountCode(invoice.getPayableAccountCode());invoice.reserve(detail.getAmount());invoices.save(invoice);}
        PayableWriteOff saved=writeOffs.save(writeOff);enqueue(saved,"PAYABLE_WRITEOFF_CREATED");return saved;}
    @Override public PayableWriteOff discardDraft(Long id){PayableWriteOff writeOff=get(id);writeOff.cancelDraft();return writeOffs.save(writeOff);}
    @Override public PayableWriteOff voidWriteOff(Long id){PayableWriteOff writeOff=get(id);if(writeOff.getStatus()==WriteOffStatus.DRAFT){return discardDraft(id);}assertBalanceChangeAllowed(writeOff.getEnterpriseId(),writeOff.getDetails().stream().map(PayableWriteOffDetail::getInvoiceId).toList());writeOff.voidWriteOff();PayableWriteOff saved=writeOffs.save(writeOff);enqueue(saved,"PAYABLE_WRITEOFF_VOID_REQUESTED");return saved;}
    @Override public void applyAccountingResult(AccountingResult result){if(!"PAYABLE_WRITEOFF".equals(result.documentType())||audit.wasProcessed(result.eventId()))return;PayableWriteOff writeOff=get(result.documentId());if(result.isVoid()){if(writeOff.getStatus()==WriteOffStatus.VOIDING&&result.accepted()){for(PayableWriteOffDetail detail:writeOff.getDetails()){SupplierInvoiceReplica invoice=locked(detail.getInvoiceId(),writeOff.getEnterpriseId());invoice.reverseWriteOff(detail.getAmount());invoices.save(invoice);}}writeOff.applyVoidResult(result.accepted());writeOffs.save(writeOff);audit.markProcessed(result.eventId(),result.tenantId());return;}if(writeOff.getStatus()!=WriteOffStatus.POSTING){audit.markProcessed(result.eventId(),result.tenantId());return;}for(PayableWriteOffDetail detail:writeOff.getDetails()){SupplierInvoiceReplica invoice=locked(detail.getInvoiceId(),writeOff.getEnterpriseId());if(result.accepted())invoice.confirmWriteOff(detail.getAmount());else invoice.release(detail.getAmount());invoices.save(invoice);}writeOff.applyResult(result.accepted(),result.accountingEntryId());writeOffs.save(writeOff);audit.markProcessed(result.eventId(),result.tenantId());}
    @Override public List<PayableWriteOff> list(String enterpriseId){return writeOffs.findByEnterprise(enterpriseId);}
    @Override public PayableWriteOff find(Long id){return get(id);}
    private PayableWriteOff get(Long id){return writeOffs.find(id).orElseThrow(()->notFound("Baja no encontrada"));}
    private SupplierInvoiceReplica locked(Long id,String enterpriseId){return invoices.findLocked(id,enterpriseId).orElseThrow(()->notFound("Obligación no encontrada"));}
    private void enqueue(PayableWriteOff value,String type){String eventId=UUID.randomUUID().toString();events.enqueue(new TreasuryEvent(eventId,"PAYABLE_WRITEOFF",value.getId(),type,value,context.tenantId(),value.getEnterpriseId(),eventId));}
    private boolean hasActiveWriteOffForInvoice(String enterpriseId,Long invoiceId){return writeOffs.findByEnterprise(enterpriseId).stream().filter(wo->blocksNewWriteOff(wo.getStatus())).flatMap(wo->wo.getDetails().stream()).anyMatch(d->d.getInvoiceId().equals(invoiceId));}
    private boolean blocksNewWriteOff(WriteOffStatus status){return status==WriteOffStatus.DRAFT||status==WriteOffStatus.POSTING||status==WriteOffStatus.VOIDING;}
    private void assertBalanceChangeAllowed(String enterpriseId, List<Long> invoiceIds) {
        if (scheduleBalanceGuard != null) {
            scheduleBalanceGuard.assertBalanceChangeAllowed(enterpriseId, invoiceIds);
        }
    }
    private TreasuryException notFound(String message){return new TreasuryException(TreasuryException.Type.NOT_FOUND,message);}private void conflict(String message){throw new TreasuryException(TreasuryException.Type.CONFLICT,message);}
}
