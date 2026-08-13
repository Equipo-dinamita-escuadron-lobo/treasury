package com.treasury.infrastructure.adapters.input.rabbit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class TreasuryRabbitDtos {
    private TreasuryRabbitDtos() {}

    public interface TenantEnvelope {
        String eventType();
        String tenantId();
        String payloadTenantId();
    }

    public record SourceDocument(String type, Long id) {}

    public record AccountingResultEnvelope(
            String eventId, String eventType, int eventVersion, Instant occurredAt,
            String tenantId, String enterpriseId, String correlationId,
            SourceDocument sourceDocument, AccountingResult payload) implements TenantEnvelope {
        @Override public String payloadTenantId() { return payload == null ? null : payload.tenantId(); }
    }

    public record AccountingResult(
            String eventId, String sourceEventId, String operation, String documentType,
            Long documentId, boolean accepted, Long accountingEntryId, String reason,
            String tenantId, String enterpriseId) {}

    public record PurchaseInvoiceEnvelope(
            String eventId, String eventType, int eventVersion, Instant occurredAt,
            String tenantId, String enterpriseId, String correlationId,
            SourceDocument sourceDocument, PurchaseInvoiceEvent payload) implements TenantEnvelope {
        @Override public String payloadTenantId() { return payload == null ? null : payload.tenantId(); }
    }

    public record PurchaseInvoiceEvent(
            Long invoiceId, String reference, String enterpriseId, Long supplierId,
            BigDecimal originalAmount, BigDecimal paidAmount, BigDecimal pendingAmount,
            LocalDate issueDate, LocalDate dueDate, Long payableAccountId,
            String payableAccountCode, boolean active, String tenantId) {}
}
