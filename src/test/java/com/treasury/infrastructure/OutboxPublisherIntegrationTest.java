package com.treasury.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import com.fasterxml.jackson.databind.JsonNode;
import com.treasury.infrastructure.adapters.output.jpa.entity.OutboxEventEntity;
import com.treasury.infrastructure.adapters.output.jpa.repository.IOutboxEventRepository;
import com.treasury.infrastructure.adapters.output.messageBroker.OutboxPublisher;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import com.treasury.infrastructure.adapters.output.security.ServiceJwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OutboxPublisherIntegrationTest {
    @Autowired IOutboxEventRepository repository;
    @Autowired OutboxPublisher publisher;
    @MockBean RabbitTemplate rabbit;
    @MockBean ServiceJwtTokenProvider tokens;

    @BeforeEach
    void tenant() {
        TenantContext.setTenantId("tenant-rabbit");
        repository.deleteAll();
        when(tokens.bearerToken()).thenReturn("Bearer signed-service-token");
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(anyString(), eq(""), any(JsonNode.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void publishesOnceWithRabbitJwtAndTenantHeadersAndMarksOutbox() throws Exception {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setEventId("event-rabbit");
        event.setAggregateType("PAYMENT_VOUCHER");
        event.setAggregateId(1L);
        event.setEventType("PAYMENT_VOUCHER_POSTED");
        event.setPayload("{\"id\":1}");
        event.setTenantId("tenant-rabbit");
        repository.saveAndFlush(event);
        clearInvocations(rabbit);

        publisher.publish();

        TenantContext.setTenantId("tenant-rabbit");
        assertThat(repository.findById(event.getId()).orElseThrow().getPublishedAt()).isNotNull();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(anyString(), eq(""),
                any(JsonNode.class), processor.capture(), any(CorrelationData.class));
        MessageProperties properties = new MessageProperties();
        Message processed = processor.getValue().postProcessMessage(new Message(new byte[0], properties));
        assertThat((Object) processed.getMessageProperties().getHeader("x-jwt-token"))
                .isEqualTo("Bearer signed-service-token");
        assertThat((Object) processed.getMessageProperties().getHeader("x-tenant-id"))
                .isEqualTo("tenant-rabbit");

        publisher.publish();
        verify(rabbit, times(1)).convertAndSend(anyString(), eq(""), any(), any(MessagePostProcessor.class), any(CorrelationData.class));
    }
}
