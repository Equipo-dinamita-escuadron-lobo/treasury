package com.treasury.infrastructure.adapters.output.messageBroker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treasury.infrastructure.adapters.config.RabbitConfig;
import com.treasury.infrastructure.adapters.output.jpa.entity.OutboxEventEntity;
import com.treasury.infrastructure.adapters.output.jpa.repository.IOutboxEventRepository;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import com.treasury.infrastructure.adapters.output.security.ServiceJwtTokenProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final IOutboxEventRepository events;
    private final RabbitTemplate rabbit;
    private final ObjectMapper mapper;
    private final ServiceJwtTokenProvider tokens;

    @Scheduled(fixedDelayString="${treasury.scheduler.outbox-delay-ms:5000}")
    @Transactional
    public void publish() {
        for (var event : events.findUnpublishedAcrossTenants()) {
            TenantContext.setTenantId(event.getTenantId());
            try {
                publishConfirmed(event);
                event.setPublishedAt(Instant.now());
                event.setStatus(OutboxEventEntity.Status.PUBLISHED);
                event.setLastError(null);
            } catch (Exception ex) {
                int attempts = event.getAttempts() + 1;
                event.setAttempts(attempts);
                event.setStatus(OutboxEventEntity.Status.FAILED);
                event.setLastError(ex.getMessage());
                long backoffSeconds = Math.min(300, 1L << Math.min(attempts, 8));
                event.setNextAttemptAt(Instant.now().plus(backoffSeconds, ChronoUnit.SECONDS));
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void publishConfirmed(OutboxEventEntity event) throws Exception {
        String exchange = "PAYMENT_VOUCHER_POSTED".equals(event.getEventType())
                ? RabbitConfig.NOTIFICATION_EXCHANGE : RabbitConfig.ACCOUNTING_EXCHANGE;
        String token = tokens.bearerToken();
        CorrelationData correlation = new CorrelationData(event.getEventId());
        rabbit.convertAndSend(exchange, "", jsonPayload(event.getPayload()), message -> {
            message.getMessageProperties().setMessageId(event.getEventId());
            message.getMessageProperties().setHeader("eventId", event.getEventId());
            message.getMessageProperties().setHeader("eventType", event.getEventType());
            message.getMessageProperties().setHeader("tenantId", event.getTenantId());
            message.getMessageProperties().setHeader("x-tenant-id", event.getTenantId());
            message.getMessageProperties().setHeader("x-jwt-token", token);
            return message;
        }, correlation);
        CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirm.isAck()) throw new IllegalStateException("RabbitMQ rechazÃ³ el evento: " + confirm.getReason());
        if (correlation.getReturned() != null) throw new IllegalStateException("RabbitMQ retornÃ³ el evento sin ruta");
    }

    private Object jsonPayload(String payload) {
        try {
            return mapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("El payload del outbox no contiene JSON vÃ¡lido", ex);
        }
    }
}
