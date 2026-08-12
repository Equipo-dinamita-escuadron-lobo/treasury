package com.treasury.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.treasury.application.output.ITreasuryEventPublisher;
import com.treasury.domain.model.TreasuryEvent;
import com.treasury.infrastructure.adapters.output.jpa.repository.IOutboxEventRepository;
import com.treasury.infrastructure.adapters.output.security.JwtAuthConverter;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;

@SpringBootTest
@ActiveProfiles("test")
class OutboxPublisherIntegrationTest {
    @Autowired ITreasuryEventPublisher publisher;
    @Autowired IOutboxEventRepository repository;
    @Autowired JwtAuthConverter jwtAuthConverter;
    @MockBean RabbitTemplate rabbit;

    @BeforeEach
    void tenant() {
        TenantContext.setTenantId("tenant-rabbit");
        repository.deleteAll();
        Jwt jwt = Jwt.withTokenValue("signed-user-token")
                .header("alg", "none")
                .subject("tenant-rabbit")
                .build();
        SecurityContextHolder.getContext().setAuthentication(jwtAuthConverter.convert(jwt));
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
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishesOnceWithHumanJwtAndTenantHeadersAndMarksOutbox() throws Exception {
        TreasuryEvent event = new TreasuryEvent(
                "event-rabbit", "PAYMENT_VOUCHER", 1L, "PAYMENT_VOUCHER_POSTED",
                java.util.Map.of("id", 1), "tenant-rabbit", "ent-1", "event-rabbit");

        publisher.enqueue(event);

        var saved = repository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getPublishedAt()).isNotNull();
        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(anyString(), eq(""),
                any(JsonNode.class), processor.capture(), any(CorrelationData.class));
        MessageProperties properties = new MessageProperties();
        Message processed = processor.getValue().postProcessMessage(new Message(new byte[0], properties));
        assertThat((Object) processed.getMessageProperties().getHeader("x-jwt-token"))
                .isEqualTo("signed-user-token");
        assertThat((Object) processed.getMessageProperties().getHeader("x-tenant-id"))
                .isEqualTo("tenant-rabbit");
    }
}
