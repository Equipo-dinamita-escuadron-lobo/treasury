package com.treasury.infrastructure.adapters.output.messageBroker.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class RabbitJwtPayloadDecoderTest {

    @Test
    void returnsSubjectFromTokenValidatedBySpringSecurityDecoder() {
        JwtDecoder signedDecoder = mock(JwtDecoder.class);
        Jwt jwt = Jwt.withTokenValue("signed-token")
                .header("alg", "RS256")
                .subject("tenant-a")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(signedDecoder.decode("signed-token")).thenReturn(jwt);

        assertThat(new RabbitJwtPayloadDecoder(signedDecoder).extractTenantId("Bearer signed-token"))
                .isEqualTo("tenant-a");
    }

    @Test
    void rejectsTokenWhenSignatureValidationFails() {
        JwtDecoder signedDecoder = mock(JwtDecoder.class);
        when(signedDecoder.decode("tampered-token"))
                .thenThrow(new JwtException("invalid signature"));

        assertThat(new RabbitJwtPayloadDecoder(signedDecoder).extractTenantId("tampered-token"))
                .isNull();
    }
}
