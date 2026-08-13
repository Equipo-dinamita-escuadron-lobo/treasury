package com.treasury.domain.model;

import java.time.Instant;

public record TreasuryEvent(String eventId, String aggregateType, Long aggregateId,
                            String eventType, Object payload, String tenantId,
                            String enterpriseId, String correlationId,
                            int eventVersion, Instant occurredAt) {
    public TreasuryEvent(String eventId, String aggregateType, Long aggregateId,
                         String eventType, Object payload, String tenantId,
                         String enterpriseId, String correlationId) {
        this(eventId, aggregateType, aggregateId, eventType, payload, tenantId,
                enterpriseId, correlationId, 1, Instant.now());
    }
}
