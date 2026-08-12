package com.treasury.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.treasury.infrastructure.adapters.output.messageBroker.aspect.JWTContextRabbitMqAspect;
import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.PurchaseInvoiceEnvelope;
import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.PurchaseInvoiceEvent;
import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.SourceDocument;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class RabbitJwtAuthenticationTest {
    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final JWTContextRabbitMqAspect aspect =
            new JWTContextRabbitMqAspect(decoder, "treasury", "facture-service", "account-catalogue-service");

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void acceptsSignedOperationalTokenAndClearsTenantAfterProcessing() throws Throwable {
        Message message = message("signed-user", "tenant-a");
        when(decoder.decode("signed-user")).thenReturn(jwt("facture-service", List.of("tenant-a")));
        ProceedingJoinPoint joinPoint = joinPoint(message, envelope("tenant-a", "tenant-a"));

        aspect.authenticateMessage(joinPoint);

        verify(joinPoint).proceed();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void rejectsTenantDifferentFromSignedUserSubject() {
        Message message = message("signed-user", "tenant-b");

        assertThatThrownBy(() -> aspect.authenticateMessage(joinPoint(message, envelope("tenant-a", "tenant-a"))))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Tenant");
    }

    @Test
    void acceptsValidatedServiceAccountForExplicitTenantHeader() throws Throwable {
        Message message = message("signed-service", "tenant-a");
        when(decoder.decode("signed-service")).thenReturn(jwt("facture-service", List.of("tenant-a")));

        aspect.authenticateMessage(joinPoint(message, envelope("tenant-a", "tenant-a")));
    }

    @Test
    void rejectsMessageWithoutJwt() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-tenant-id", "tenant-a");

        assertThatThrownBy(() -> aspect.authenticateMessage(joinPoint(new Message(new byte[0], properties), envelope("tenant-a", "tenant-a"))))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("x-jwt-token");
    }

    @Test
    void rejectsWildcardTenantClaim() {
        Message message = message("signed-service", "tenant-a");
        when(decoder.decode("signed-service")).thenReturn(jwt("facture-service", List.of("*")));
        assertThatThrownBy(() -> aspect.authenticateMessage(joinPoint(message, envelope("tenant-a", "tenant-a"))))
                .isInstanceOf(SecurityException.class).hasMessageContaining("comodines");
    }

    @Test
    void rejectsUnauthorizedProducer() {
        Message message = message("signed-service", "tenant-a");
        when(decoder.decode("signed-service")).thenReturn(jwt("treasury-service", List.of("tenant-a")));
        assertThatThrownBy(() -> aspect.authenticateMessage(joinPoint(message, envelope("tenant-a", "tenant-a"))))
                .isInstanceOf(SecurityException.class).hasMessageContaining("azp");
    }

    private ProceedingJoinPoint joinPoint(Message message, PurchaseInvoiceEnvelope envelope) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {envelope, message});
        return joinPoint;
    }

    private Message message(String token, String tenant) {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-jwt-token", "Bearer " + token);
        properties.setHeader("x-tenant-id", tenant);
        return new Message(new byte[0], properties);
    }

    private PurchaseInvoiceEnvelope envelope(String envelopeTenant, String payloadTenant) {
        PurchaseInvoiceEvent payload = new PurchaseInvoiceEvent(1L, "FC-1", "ent", 2L,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, LocalDate.now(), LocalDate.now(),
                3L, "2205", true, payloadTenant);
        return new PurchaseInvoiceEnvelope("event", "PURCHASE_INVOICE_CREATED", 1, Instant.now(),
                envelopeTenant, "ent", "correlation", new SourceDocument("PURCHASE_INVOICE", 1L), payload);
    }

    private Jwt jwt(String azp, List<String> tenantIds) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("service-subject")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .audience(List.of("treasury"))
                .claim("azp", azp)
                .claim("tenant_ids", tenantIds)
                .build();
    }
}
