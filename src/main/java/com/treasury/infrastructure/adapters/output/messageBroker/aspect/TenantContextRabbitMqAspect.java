package com.treasury.infrastructure.adapters.output.messageBroker.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;

@Aspect
@Component
public class TenantContextRabbitMqAspect {

    private static final Logger logger = LoggerFactory.getLogger(TenantContextRabbitMqAspect.class);
    private static final String TENANT_HEADER = "x-tenant-id";

    /**
     * Este "advice" se ejecuta alrededor de cualquier método anotado con @RabbitListener.
     * Su función es extraer el tenantId de las cabeceras del mensaje, establecerlo
     * en el TenantContext, ejecutar el método listener y finalmente limpiar el contexto.
     */
    @Around("@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
    public Object setTenantContext(ProceedingJoinPoint joinPoint) throws Throwable {

        // Buscamos el objeto 'Message' en los argumentos del método del listener
        Message message = findMessageArgument(joinPoint.getArgs());

        if (message == null) {
            logger.warn("El listener [{}] no recibe un objeto 'Message'. No se puede establecer el contexto del tenant.", joinPoint.getSignature().getName());
            return joinPoint.proceed(); // Ejecutar sin contexto
        }

        String tenantId = (String) message.getMessageProperties().getHeaders().get(TENANT_HEADER);

        if (tenantId == null || tenantId.isEmpty()) {
            logger.error("Mensaje recibido sin la cabecera '{}'. Se procesará sin contexto de tenant.", TENANT_HEADER);
            return joinPoint.proceed(); // Ejecutar sin contexto
        }

        try {
            // 1. Establecer el contexto del Tenant para este hilo
            TenantContext.setTenantId(tenantId);
            logger.debug("Contexto de tenant '{}' establecido para el listener [{}].", tenantId, joinPoint.getSignature().getName());

            // 2. Ejecutar el método original del listener
            return joinPoint.proceed();

        } finally {
            logger.debug("Limpiando el contexto del tenant '{}'.", tenantId);
            TenantContext.clear();
        }
    }

    /**
     * Método de utilidad para encontrar el argumento de tipo Message.
     */
    private Message findMessageArgument(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Message) {
                return (Message) arg;
            }
        }
        return null;
    }
}
