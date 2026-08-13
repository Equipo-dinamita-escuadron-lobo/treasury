package com.treasury.infrastructure.adapters.output.messageBroker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treasury.application.output.ITreasuryEventPublisher;
import com.treasury.domain.model.TreasuryEvent;
import com.treasury.infrastructure.adapters.output.jpa.entity.OutboxEventEntity;
import com.treasury.infrastructure.adapters.output.jpa.repository.IOutboxEventRepository;
import com.treasury.infrastructure.adapters.output.messageBroker.support.TreasuryOutboxMessenger;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class OutboxEventAdapter implements ITreasuryEventPublisher {
    private final IOutboxEventRepository repository;
    private final ObjectMapper mapper;
    private final TreasuryOutboxMessenger messenger;

    @Override
    public void enqueue(TreasuryEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setEventId(event.eventId());
        entity.setAggregateType(event.aggregateType());
        entity.setAggregateId(event.aggregateId());
        entity.setEventType(event.eventType());
        entity.setPayload(buildPayload(event));
        entity.setTenantId(event.tenantId());
        repository.save(entity);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishSaved(event.eventId());
                }
            });
        } else {
            publishSaved(event.eventId());
        }
    }

    private void publishSaved(String eventId) {
        OutboxEventEntity entity = repository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException("Outbox no encontrado: " + eventId));
        try {
            messenger.publishNow(entity);
            entity.setPublishedAt(Instant.now());
            entity.setStatus(OutboxEventEntity.Status.PUBLISHED);
            entity.setLastError(null);
            repository.save(entity);
        } catch (Exception ex) {
            entity.setStatus(OutboxEventEntity.Status.FAILED);
            entity.setLastError(ex.getMessage());
            entity.setAttempts(entity.getAttempts() + 1);
            repository.save(entity);
            throw new IllegalStateException("No fue posible publicar el evento de tesorería", ex);
        }
    }

    private String buildPayload(TreasuryEvent event) {
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", event.eventId());
            envelope.put("eventType", event.eventType());
            envelope.put("eventVersion", event.eventVersion());
            envelope.put("occurredAt", event.occurredAt());
            envelope.put("tenantId", event.tenantId());
            envelope.put("enterpriseId", event.enterpriseId());
            envelope.put("correlationId", event.correlationId());
            envelope.put("sourceDocument", Map.of("type", event.aggregateType(), "id", event.aggregateId()));
            envelope.put("payload", event.payload());
            return mapper.writeValueAsString(envelope);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible serializar el evento", ex);
        }
    }
}
