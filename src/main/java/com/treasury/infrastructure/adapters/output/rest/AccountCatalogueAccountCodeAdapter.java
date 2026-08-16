package com.treasury.infrastructure.adapters.output.rest;

import com.treasury.application.output.IAccountCodeResolverPort;
import com.treasury.domain.model.AccountCatalogueAccountSnapshot;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import com.treasury.infrastructure.adapters.output.security.IJwtUtils;
import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class AccountCatalogueAccountCodeAdapter implements IAccountCodeResolverPort {
    private final RestClient client;
    private final IJwtUtils jwtUtils;

    public AccountCatalogueAccountCodeAdapter(
            @Value("${treasury.integrations.account-catalogue-url:http://localhost:8080}") String baseUrl,
            IJwtUtils jwtUtils) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Optional<String> resolveCode(Long accountId, String enterpriseId) {
        return resolveAccount(accountId, enterpriseId).map(AccountCatalogueAccountSnapshot::code);
    }

    @Override
    public Optional<AccountCatalogueAccountSnapshot> resolveAccount(Long accountId, String enterpriseId) {
        if (accountId == null || enterpriseId == null || enterpriseId.isBlank()) {
            return Optional.empty();
        }
        try {
            AccountItem[] items = client.get()
                    .uri("/api/accountCatalogue/search/{enterpriseId}", enterpriseId)
                    .header("Authorization", "Bearer " + jwtUtils.getToken())
                    .header("X-Tenant-ID", Optional.ofNullable(TenantContext.getTenantId()).orElse(""))
                    .retrieve()
                    .body(AccountItem[].class);
            if (items == null) {
                return Optional.empty();
            }
            return Arrays.stream(items)
                    .filter(a -> accountId.equals(a.id()))
                    .findFirst()
                    .map(a -> new AccountCatalogueAccountSnapshot(a.code(), a.description(), a.classification(),
                            Boolean.TRUE.equals(a.status())));
        } catch (Exception ex) {
            log.warn("No se pudo resolver cuenta PUC {}: {}", accountId, ex.getMessage());
            return Optional.empty();
        }
    }

    private record AccountItem(Long id, String code, String description, Integer classification, Boolean status) {}
}
