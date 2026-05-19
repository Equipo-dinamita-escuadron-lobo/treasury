package com.treasury.infrastructure.adapters.output.messageBroker;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.treasury.copy.infrastructure.adapters.output.persistence.jpa.CopyActiveFlag;
import com.treasury.domain.model.Treasury;
import com.treasury.domain.port.ITreasuryCommandRepositoryPort;
import com.treasury.infrastructure.adapters.config.RabbitConfig;
import com.treasury.infrastructure.adapters.output.messageBroker.dto.EventDto;
import com.treasury.infrastructure.adapters.output.messageBroker.dto.TreasuryDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TreasuryListener{
    private final ITreasuryCommandRepositoryPort treasuryCommandPort;
    private final CopyActiveFlag copyActiveFlag;

    @RabbitListener(queues = RabbitConfig.TREASURY_TRANSACTION_QUEUE)
    public void handleTreasuryEvent(EventDto<TreasuryDto> event, Message message) {
        // Suprimir mensajes AMQP durante copia activa para evitar corrupción del tenant destino (ADR-38)
        if (copyActiveFlag.isActive()) {
            log.debug("Copia treasury en curso — mensaje AMQP suprimido: {}", event.getData());
            return;
        }
        log.info("Received treasury event: {}", event.getData());

        switch (event.getType()) {
            case CREATED:
                Treasury treasury = new Treasury(
                    event.getData().getAccountNumber(),
                    event.getData().getCurrency(),
                    event.getData().getAccountType()
                );
                treasuryCommandPort.save(treasury);
                break;
            case UPDATED:

                break;
            case DELETED:

                break;
        }
    }
}
