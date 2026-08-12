package com.treasury.infrastructure.adapters.output.security;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class ServiceJwtTokenProvider {
    private final RestClient restClient = RestClient.create();
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private volatile CachedToken cached;

    public ServiceJwtTokenProvider(
            @Value("${treasury.security.service-token.uri}") String tokenUri,
            @Value("${treasury.security.service-token.client-id}") String clientId,
            @Value("${treasury.security.service-token.client-secret:}") String clientSecret) {
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String bearerToken() {
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt().isAfter(now.plusSeconds(30))) {
            return cached.value();
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("TREASURY_OAUTH_CLIENT_SECRET no está configurado");
        }
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        TokenResponse response = restClient.post().uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(TokenResponse.class);
        if (response == null || response.access_token() == null || response.access_token().isBlank()) {
            throw new IllegalStateException("Keycloak no devolvió un access_token de servicio");
        }
        cached = new CachedToken("Bearer " + response.access_token(),
                now.plusSeconds(Math.max(1, response.expires_in())));
        return cached.value();
    }

    private record TokenResponse(String access_token, long expires_in) {}
    private record CachedToken(String value, Instant expiresAt) {}
}
