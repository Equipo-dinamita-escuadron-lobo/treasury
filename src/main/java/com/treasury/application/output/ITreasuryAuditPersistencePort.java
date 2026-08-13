package com.treasury.application.output;

import java.time.LocalDate;

public interface ITreasuryAuditPersistencePort {
    boolean wasProcessed(String eventId);
    void markProcessed(String eventId, String tenantId);
    void recordDueDate(Long invoiceId, LocalDate previousDate, LocalDate newDate,
                       String reason, String changedBy, String tenantId);
}
