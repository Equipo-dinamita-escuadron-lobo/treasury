package com.treasury.infrastructure.adapters.output.jpa.adapter;

import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.infrastructure.adapters.output.jpa.entity.*;
import com.treasury.infrastructure.adapters.output.jpa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component @RequiredArgsConstructor
public class TreasuryAuditJpaAdapter implements ITreasuryAuditPersistencePort {
    private final IProcessedEventRepository processed;
    private final IDueDateHistoryRepository dueDates;
    @Override public boolean wasProcessed(String eventId){return processed.existsByEventId(eventId);}
    @Override public void markProcessed(String eventId,String tenantId){ProcessedEventEntity value=new ProcessedEventEntity();value.setEventId(eventId);value.setTenantId(tenantId);processed.save(value);}
    @Override public void recordDueDate(Long invoiceId,LocalDate previous,LocalDate next,String reason,String user,String tenantId){DueDateHistoryEntity value=new DueDateHistoryEntity();value.setInvoiceId(invoiceId);value.setPreviousDate(previous);value.setNewDate(next);value.setReason(reason);value.setChangedBy(user);value.setTenantId(tenantId);dueDates.save(value);}
}
