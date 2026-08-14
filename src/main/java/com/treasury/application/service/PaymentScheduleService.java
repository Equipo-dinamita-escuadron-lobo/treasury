package com.treasury.application.service;

import com.treasury.application.input.IPaymentScheduleCommandUseCase;
import com.treasury.application.input.IPaymentScheduleExecutionUseCase;
import com.treasury.application.input.IPaymentScheduleQueryUseCase;
import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.application.output.*;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.*;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
public class PaymentScheduleService implements IPaymentScheduleCommandUseCase,
        IPaymentScheduleQueryUseCase, IPaymentScheduleExecutionUseCase {
    private final IPaymentSchedulePersistencePort schedules;
    private final ISupplierInvoiceProviderPort invoices;
    private final IPaymentVoucherCommandUseCase voucherCommands;
    private final IPaymentVoucherQueryUseCase voucherQueries;
    private final IPaymentVoucherQueryPersistencePort voucherQueryPersistence;
    private final IExecutionContextPort context;
    private final ITransactionRunnerPort transactions;
    private final ITimeProviderPort time;
    private final IPaymentMethodProviderPort paymentMethods;

    @Override public PaymentSchedule create(Schedule command) {
        PaymentSchedule schedule=new PaymentSchedule();schedule.setStatus(PaymentScheduleStatus.SCHEDULED);schedule.setTenantId(context.tenantId());apply(schedule,command);return schedules.save(schedule);
    }
    @Override public PaymentSchedule update(Long id,Schedule command){PaymentSchedule schedule=locked(id);schedule.ensureEditable();schedule.setStatus(PaymentScheduleStatus.SCHEDULED);schedule.setFailureReason(null);apply(schedule,command);return schedules.save(schedule);}
    private void apply(PaymentSchedule schedule,Schedule command){
        validatePaymentMethod(command.paymentMethodId(),command.bankAccountId(),command.enterpriseId());
        schedule.setEnterpriseId(command.enterpriseId());schedule.setExecutionDate(command.executionDate());schedule.setPaymentMethodId(command.paymentMethodId());schedule.setBankAccountId(command.bankAccountId());schedule.setObservations(command.observations());
        Set<Long> unique=new HashSet<>();List<PaymentScheduleDetail> details=new ArrayList<>();
        for(Detail requested:command.details()){
            if(requested.amount()==null||requested.amount().signum()<=0)throw new TreasuryException(TreasuryException.Type.BAD_REQUEST,"El valor debe ser mayor que cero");
            if(!unique.add(requested.invoiceId()))conflict("Una factura no puede repetirse");
            SupplierInvoiceReplica invoice=invoices.findById(requested.invoiceId()).filter(i->i.getEnterpriseId().equals(command.enterpriseId())&&i.isActive()).orElseThrow(()->notFound("Obligación no encontrada"));
            if(!invoice.getSupplierId().equals(requested.supplierId())||requested.amount().compareTo(invoice.available())>0)conflict("Detalle programado inválido");
            PaymentScheduleDetail detail=new PaymentScheduleDetail();detail.setSupplierId(invoice.getSupplierId());detail.setInvoiceId(invoice.getId());detail.setAmount(requested.amount());detail.setTenantId(context.tenantId());details.add(detail);
        }schedule.setDetails(details);
    }
    @Override public void delete(Long id){PaymentSchedule schedule=locked(id);schedule.ensureEditable();schedules.delete(schedule);}
    @Override public PaymentSchedule cancel(Long id){PaymentSchedule schedule=locked(id);schedule.cancel();return schedules.save(schedule);}
    @Override public PaymentSchedule find(Long id){return schedules.find(id).orElseThrow(()->notFound("Programación no encontrada"));}
    @Override public List<PaymentSchedule> list(ScheduleFilter filter){return schedules.search(filter);}
    @Override public PaymentSchedule execute(Long id){
        PaymentSchedule schedule=locked(id);if(schedule.getStatus()!=PaymentScheduleStatus.SCHEDULED&&schedule.getStatus()!=PaymentScheduleStatus.FAILED)return schedule;schedule.start();schedule=schedules.save(schedule);
        try{
            PaymentVoucher voucher;
            if(schedule.getVoucherId()==null){Voucher command=new Voucher(schedule.getEnterpriseId(),time.today(),schedule.getPaymentMethodId(),schedule.getBankAccountId(),schedule.getObservations(),schedule.getDetails().stream().map(d->new Detail(d.getSupplierId(),d.getInvoiceId(),d.getAmount())).toList());voucher=voucherCommands.create(command);schedule.setVoucherId(voucher.getId());schedule=schedules.save(schedule);}else voucher=voucherQueries.find(schedule.getVoucherId(),schedule.getEnterpriseId());
            voucherCommands.post(voucher.getId(),schedule.getEnterpriseId(),"schedule-"+schedule.getId()+"-"+schedule.getRetryCount());schedule.waitingAccounting();
        }catch(RuntimeException ex){schedule.failed(ex.getMessage());}
        return schedules.save(schedule);
    }
    @Override public void executeDue(LocalDate date){for(DuePaymentSchedule due:schedules.findDue(date)){context.runAsTenant(due.tenantId(),()->transactions.run(()->execute(due.id())));}}
    @Override public void recoverAbandoned(Instant before){
        for(DuePaymentSchedule due:schedules.findAbandoned(before)){
            context.runAsTenant(due.tenantId(),()->transactions.run(()->{
                PaymentSchedule schedule=locked(due.id());
                if(schedule.getStatus()==PaymentScheduleStatus.PROCESSING){
                    schedule.failed("Ejecucion abandonada recuperada por el scheduler");
                    schedules.save(schedule);
                }
            }));
        }
        for(DuePaymentSchedule due:schedules.findWaitingAccounting(before)){
            context.runAsTenant(due.tenantId(),()->transactions.run(()->reconcileWaitingAccounting(due.id())));
        }
    }
    @Override public void applyAccountingResult(AccountingResult result){
        if(result.isVoid()||!"PAYMENT_VOUCHER".equals(result.documentType()))return;
        schedules.findByVoucherId(result.documentId()).ifPresent(schedule->{
            if(schedule.getStatus()==PaymentScheduleStatus.WAITING_ACCOUNTING){
                schedule.accountingResult(result.accepted(),result.reason());
                schedules.save(schedule);
            }
        });
    }
    private void reconcileWaitingAccounting(Long scheduleId){
        PaymentSchedule schedule=locked(scheduleId);
        if(schedule.getStatus()!=PaymentScheduleStatus.WAITING_ACCOUNTING||schedule.getVoucherId()==null)return;
        voucherQueryPersistence.findById(schedule.getVoucherId()).ifPresent(voucher->{
            if(voucher.getStatus()==PaymentVoucherStatus.FAILED){
                schedule.failed(voucher.getFailureReason()==null||voucher.getFailureReason().isBlank()
                        ? "Contabilización rechazada" : voucher.getFailureReason());
                schedules.save(schedule);
            }else if(voucher.getStatus()==PaymentVoucherStatus.POSTED){
                schedule.accountingResult(true,null);
                schedules.save(schedule);
            }
        });
    }
    private PaymentSchedule locked(Long id){return schedules.findLocked(id).orElseThrow(()->notFound("Programación no encontrada"));}
    private void validatePaymentMethod(Long methodId,Long bankId,String enterpriseId){paymentMethods.validateForPayment(methodId,bankId,enterpriseId);}
    private TreasuryException notFound(String message){return new TreasuryException(TreasuryException.Type.NOT_FOUND,message);}private void conflict(String message){throw new TreasuryException(TreasuryException.Type.CONFLICT,message);}
}
