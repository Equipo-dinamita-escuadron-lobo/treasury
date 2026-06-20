package com.treasury.infrastructure.adapters.output.messageBroker;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.treasury.copy.infrastructure.adapters.output.persistence.jpa.CopyActiveFlag;
import com.treasury.infrastructure.adapters.config.RabbitConfig;
import com.treasury.infrastructure.adapters.output.messageBroker.dto.EventDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TreasuryListener {

    private final CopyActiveFlag copyActiveFlag;

    @RabbitListener(queues = RabbitConfig.TREASURY_TRANSACTION_QUEUE)
    public void handleTreasuryEvent(EventDto<?> event, Message message) {
        // Suprimir mensajes AMQP durante copia activa para evitar corrupción del tenant destino (ADR-38)
        if (copyActiveFlag.isActive()) {
            log.debug("Copia treasury en curso — mensaje AMQP suprimido: {}", event.getData());
            return;
        }
        log.info("Received treasury event: {}", event.getData());
    }
}
