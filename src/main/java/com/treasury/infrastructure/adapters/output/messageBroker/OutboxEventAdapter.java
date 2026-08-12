package com.treasury.infrastructure.adapters.output.messageBroker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treasury.application.output.ITreasuryEventPublisher;
import com.treasury.domain.model.TreasuryEvent;
import com.treasury.infrastructure.adapters.output.jpa.entity.OutboxEventEntity;
import com.treasury.infrastructure.adapters.output.jpa.repository.IOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

@Component @RequiredArgsConstructor
public class OutboxEventAdapter implements ITreasuryEventPublisher {
    private final IOutboxEventRepository repository;
    private final ObjectMapper mapper;
    @Override public void enqueue(TreasuryEvent event){try{OutboxEventEntity entity=new OutboxEventEntity();entity.setEventId(event.eventId());entity.setAggregateType(event.aggregateType());entity.setAggregateId(event.aggregateId());entity.setEventType(event.eventType());Map<String,Object> envelope=new LinkedHashMap<>();envelope.put("eventId",event.eventId());envelope.put("eventType",event.eventType());envelope.put("eventVersion",event.eventVersion());envelope.put("occurredAt",event.occurredAt());envelope.put("tenantId",event.tenantId());envelope.put("enterpriseId",event.enterpriseId());envelope.put("correlationId",event.correlationId());envelope.put("sourceDocument",Map.of("type",event.aggregateType(),"id",event.aggregateId()));envelope.put("payload",event.payload());entity.setPayload(mapper.writeValueAsString(envelope));entity.setTenantId(event.tenantId());repository.save(entity);}catch(Exception ex){throw new IllegalStateException("No fue posible serializar el evento",ex);}}
}
