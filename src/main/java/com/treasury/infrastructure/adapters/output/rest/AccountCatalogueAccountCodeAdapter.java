package com.treasury.infrastructure.adapters.output.rest;

import com.treasury.application.output.IAccountCodeResolverPort;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import com.treasury.infrastructure.adapters.output.security.ServiceJwtTokenProvider;
import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class AccountCatalogueAccountCodeAdapter implements IAccountCodeResolverPort {
    private final RestClient client;
    private final ServiceJwtTokenProvider serviceTokens;

    public AccountCatalogueAccountCodeAdapter(
            @Value("${treasury.integrations.account-catalogue-url:http://localhost:8080}") String baseUrl,
            ServiceJwtTokenProvider serviceTokens) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.serviceTokens = serviceTokens;
    }

    @Override
    public Optional<String> resolveCode(Long accountId, String enterpriseId) {
        if (accountId == null || enterpriseId == null || enterpriseId.isBlank()) {
            return Optional.empty();
        }
        try {
            AccountItem[] items = client.get()
                    .uri("/api/accountCatalogue/search/{enterpriseId}", enterpriseId)
                    .header("Authorization", bearer())
                    .header("X-Tenant-ID", Optional.ofNullable(TenantContext.getTenantId()).orElse(""))
                    .retrieve()
                    .body(AccountItem[].class);
            if (items == null) {
                return Optional.empty();
            }
            return Arrays.stream(items)
                    .filter(a -> accountId.equals(a.id()))
                    .map(AccountItem::code)
                    .filter(code -> code != null && !code.isBlank() && !code.equals(String.valueOf(accountId)))
                    .findFirst();
        } catch (Exception ex) {
            log.warn("No se pudo resolver código PUC para cuenta {}: {}", accountId, ex.getMessage());
            return Optional.empty();
        }
    }

    private String bearer() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return "Bearer " + jwt.getToken().getTokenValue();
        }
        return serviceTokens.bearerToken();
    }

    private record AccountItem(Long id, String code) {}
}
