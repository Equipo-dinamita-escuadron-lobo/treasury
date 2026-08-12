package com.treasury.infrastructure.adapters.output.messageBroker.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitJwtPayloadDecoder {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractTenantId(String jwtToken) {
        try {
            String token = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
            String[] chunks = token.split("\\.");
            if (chunks.length != 3) {
                log.error("Token JWT inválido: no tiene el formato correcto");
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(chunks[1]));
            JsonNode jsonNode = objectMapper.readTree(payload);
            JsonNode subNode = jsonNode.get("sub");
            if (subNode != null) {
                return subNode.asText();
            }
            log.warn("No se encontró el claim 'sub' en el token JWT");
            return null;
        } catch (Exception e) {
            log.error("Error al decodificar el token JWT: {}", e.getMessage(), e);
            return null;
        }
    }
}
