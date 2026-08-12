package com.treasury.infrastructure.adapters.output.messageBroker.aspect;

import com.rabbitmq.client.LongString;
import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.TenantEnvelope;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import java.util.Collection;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class JWTContextRabbitMqAspect {
    private final JwtDecoder jwtDecoder;
    private final String expectedAudience;
    private final String factureAzp;
    private final String accountingAzp;

    public JWTContextRabbitMqAspect(JwtDecoder jwtDecoder,
            @Value("${treasury.security.rabbit.expected-audience:treasury}") String expectedAudience,
            @Value("${treasury.security.rabbit.facture-azp:facture-service}") String factureAzp,
            @Value("${treasury.security.rabbit.accounting-azp:account-catalogue-service}") String accountingAzp) {
        this.jwtDecoder = jwtDecoder;
        this.expectedAudience = expectedAudience;
        this.factureAzp = factureAzp;
        this.accountingAzp = accountingAzp;
    }

    @Around("@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
    public Object authenticateMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        Message message = find(joinPoint.getArgs(), Message.class);
        TenantEnvelope envelope = find(joinPoint.getArgs(), TenantEnvelope.class);
        if (message == null || envelope == null) throw new SecurityException("Listener PP8 sin mensaje o envelope");
        String headerTenant = requiredHeader(message, "x-tenant-id");
        if (!headerTenant.equals(envelope.tenantId()) || !headerTenant.equals(envelope.payloadTenantId())) {
            throw new SecurityException("Tenant inconsistente entre header, envelope y payload");
        }
        String encoded = requiredHeader(message, "x-jwt-token");
        Jwt jwt = jwtDecoder.decode(encoded.startsWith("Bearer ") ? encoded.substring(7) : encoded);
        if (jwt.getAudience() == null || !jwt.getAudience().contains(expectedAudience)) {
            throw new SecurityException("Audience invÃ¡lida para TesorerÃ­a");
        }
        String expectedAzp = "ACCOUNTING_RESULT".equals(envelope.eventType()) ? accountingAzp : factureAzp;
        if (!expectedAzp.equals(jwt.getClaimAsString("azp"))) {
            throw new SecurityException("azp no autorizado para el tipo de evento PP8");
        }
        Collection<String> tenantIds = stringCollection(jwt.getClaim("tenant_ids"));
        if (tenantIds.contains("*")) throw new SecurityException("tenant_ids no admite comodines");
        if (!tenantIds.contains(headerTenant)) throw new SecurityException("Tenant no autorizado por tenant_ids");
        TenantContext.setTenantId(headerTenant);
        try {
            return joinPoint.proceed();
        } finally {
            TenantContext.clear();
        }
    }

    private Collection<String> stringCollection(Object claim) {
        if (claim instanceof Collection<?> values) return values.stream().map(String::valueOf).toList();
        if (claim instanceof String value && !value.isBlank()) return List.of(value);
        return List.of();
    }

    private <T> T find(Object[] args, Class<T> type) {
        for (Object arg : args) if (type.isInstance(arg)) return type.cast(arg);
        return null;
    }

    private String requiredHeader(Message message, String name) {
        Object value = message.getMessageProperties().getHeaders().get(name);
        if (value instanceof LongString longString) return longString.toString();
        if (value instanceof String text && !text.isBlank()) return text;
        throw new SecurityException("Falta header Rabbit requerido: " + name);
    }
}
