package com.treasury.infrastructure.adapters.output.messageBroker.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitJwtPayloadDecoder {
    private final JwtDecoder jwtDecoder;

    public String extractTenantId(String jwtToken) {
        try {
            String token = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
            String subject = jwtDecoder.decode(token).getSubject();
            if (subject != null && !subject.isBlank()) {
                return subject;
            }
            log.warn("No se encontró el claim 'sub' en el token JWT");
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT Rabbit inválido: {}", e.getMessage());
            return null;
        }
    }
}
