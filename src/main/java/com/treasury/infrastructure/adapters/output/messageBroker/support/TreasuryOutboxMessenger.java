package com.treasury.infrastructure.adapters.output.messageBroker.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treasury.infrastructure.adapters.config.RabbitConfig;
import com.treasury.infrastructure.adapters.output.jpa.entity.OutboxEventEntity;
import com.treasury.infrastructure.adapters.output.security.IJwtUtils;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TreasuryOutboxMessenger {
    private final RabbitTemplate rabbit;
    private final ObjectMapper mapper;
    private final IJwtUtils jwtUtils;

    public void publishNow(OutboxEventEntity event) throws Exception {
        String exchange = "PAYMENT_VOUCHER_POSTED".equals(event.getEventType())
                ? RabbitConfig.NOTIFICATION_EXCHANGE : RabbitConfig.ACCOUNTING_EXCHANGE;
        String jwtToken = resolveJwtToken();
        CorrelationData correlation = new CorrelationData(event.getEventId());
        rabbit.convertAndSend(exchange, "", jsonPayload(event.getPayload()), applyHeaders(event, jwtToken), correlation);
        CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ rechazó el evento: " + confirm.getReason());
        }
        if (correlation.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ retornó el evento sin ruta");
        }
    }

    private MessagePostProcessor applyHeaders(OutboxEventEntity event, String jwtToken) {
        return message -> {
            message.getMessageProperties().setMessageId(event.getEventId());
            message.getMessageProperties().setHeader("eventId", event.getEventId());
            message.getMessageProperties().setHeader("eventType", event.getEventType());
            message.getMessageProperties().setHeader("tenantId", event.getTenantId());
            message.getMessageProperties().setHeader("x-tenant-id", event.getTenantId());
            if (jwtToken != null && !jwtToken.isBlank()) {
                message.getMessageProperties().setHeader("x-jwt-token", jwtToken);
            }
            return message;
        };
    }

    private String resolveJwtToken() {
        try {
            return jwtUtils.getToken();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    private Object jsonPayload(String payload) {
        try {
            return mapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("El payload del outbox no contiene JSON válido", ex);
        }
    }
}
