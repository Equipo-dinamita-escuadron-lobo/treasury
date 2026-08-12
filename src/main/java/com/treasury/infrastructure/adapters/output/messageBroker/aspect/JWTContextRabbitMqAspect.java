package com.treasury.infrastructure.adapters.output.messageBroker.aspect;

import com.rabbitmq.client.LongString;
import com.treasury.infrastructure.adapters.output.messageBroker.support.RabbitJwtPayloadDecoder;
import com.treasury.infrastructure.adapters.output.messageBroker.support.JwtTokenService;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class JWTContextRabbitMqAspect {
    private static final Logger logger = LoggerFactory.getLogger(JWTContextRabbitMqAspect.class);
    private static final String JWT_TOKEN_HEADER = "x-jwt-token";

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RabbitJwtPayloadDecoder jwtDecoder;

    @Around("@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
    public Object setTenantContext(ProceedingJoinPoint joinPoint) throws Throwable {
        Message message = findMessageArgument(joinPoint.getArgs());
        if (message == null) {
            logger.warn("El listener [{}] no recibe un objeto 'Message'. No se puede establecer el contexto del tenant.",
                    joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }

        Object tokenObject = message.getMessageProperties().getHeaders().get(JWT_TOKEN_HEADER);
        String jwtToken = extractTokenFromObject(tokenObject);
        if (jwtToken == null || jwtToken.isBlank()) {
            logger.error("Mensaje recibido sin la cabecera '{}'. Se procesará sin contexto de tenant.", JWT_TOKEN_HEADER);
            return joinPoint.proceed();
        }

        try {
            String tenantId = jwtDecoder.extractTenantId(jwtToken);
            if (tenantId == null || tenantId.isBlank()) {
                logger.error("No se pudo extraer el tenant ID del token JWT. Se procesará sin contexto de tenant.");
                return joinPoint.proceed();
            }
            jwtTokenService.setRabbitJwtToken(jwtToken);
            jwtTokenService.setRabbitTenantId(tenantId);
            TenantContext.setTenantId(tenantId);
            logger.info("Contexto de tenant '{}' establecido para el listener [{}].", tenantId,
                    joinPoint.getSignature().getName());
            return joinPoint.proceed();
        } finally {
            TenantContext.clear();
            jwtTokenService.clearRabbitContext();
        }
    }

    private Message findMessageArgument(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Message message) {
                return message;
            }
        }
        return null;
    }

    private String extractTokenFromObject(Object tokenObject) {
        if (tokenObject instanceof LongString longString) {
            return longString.toString();
        }
        if (tokenObject instanceof String text) {
            return text;
        }
        return null;
    }
}
