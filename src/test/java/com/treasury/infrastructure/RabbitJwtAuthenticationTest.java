package com.treasury.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.PurchaseInvoiceEnvelope;
import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.PurchaseInvoiceEvent;
import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.SourceDocument;
import com.treasury.infrastructure.adapters.output.messageBroker.aspect.JWTContextRabbitMqAspect;
import com.treasury.infrastructure.adapters.output.messageBroker.support.RabbitJwtPayloadDecoder;
import com.treasury.infrastructure.adapters.output.messageBroker.support.JwtTokenService;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.test.util.ReflectionTestUtils;

class RabbitJwtAuthenticationTest {
    private final RabbitJwtPayloadDecoder decoder = mock(RabbitJwtPayloadDecoder.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final JWTContextRabbitMqAspect aspect = new JWTContextRabbitMqAspect();

    @BeforeEach
    void wireAspect() {
        ReflectionTestUtils.setField(aspect, "jwtDecoder", decoder);
        ReflectionTestUtils.setField(aspect, "jwtTokenService", jwtTokenService);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void acceptsSignedOperationalTokenAndClearsTenantAfterProcessing() throws Throwable {
        Message message = message("signed-user", "tenant-a");
        when(decoder.extractTenantId("signed-user")).thenReturn("tenant-a");
        ProceedingJoinPoint joinPoint = joinPoint(message, envelope("tenant-a"));

        aspect.setTenantContext(joinPoint);

        verify(joinPoint).proceed();
        verify(jwtTokenService).setRabbitJwtToken("signed-user");
        verify(jwtTokenService).setRabbitTenantId("tenant-a");
        verify(jwtTokenService).clearRabbitContext();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void proceedsWithoutTenantWhenJwtHeaderIsMissing() throws Throwable {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-tenant-id", "tenant-a");
        ProceedingJoinPoint joinPoint = joinPoint(new Message(new byte[0], properties), envelope("tenant-a"));

        aspect.setTenantContext(joinPoint);

        verify(joinPoint).proceed();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    private ProceedingJoinPoint joinPoint(Message message, PurchaseInvoiceEnvelope envelope) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("listen");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {envelope, message});
        return joinPoint;
    }

    private Message message(String token, String tenant) {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-jwt-token", token);
        properties.setHeader("x-tenant-id", tenant);
        return new Message(new byte[0], properties);
    }

    private PurchaseInvoiceEnvelope envelope(String tenant) {
        PurchaseInvoiceEvent payload = new PurchaseInvoiceEvent(1L, "FC-1", "ent", 2L,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, LocalDate.now(), LocalDate.now(),
                3L, "2205", true, tenant);
        return new PurchaseInvoiceEnvelope("event", "PURCHASE_INVOICE_CREATED", 1, Instant.now(),
                tenant, "ent", "correlation", new SourceDocument("PURCHASE_INVOICE", 1L), payload);
    }
}
