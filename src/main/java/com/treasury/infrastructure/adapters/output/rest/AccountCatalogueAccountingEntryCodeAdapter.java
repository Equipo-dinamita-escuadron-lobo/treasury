package com.treasury.infrastructure.adapters.output.rest;

import com.treasury.application.output.IAccountingEntryCodePort;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import com.treasury.infrastructure.adapters.output.security.IJwtUtils;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class AccountCatalogueAccountingEntryCodeAdapter implements IAccountingEntryCodePort {
    private final RestClient client;
    private final IJwtUtils jwtUtils;

    public AccountCatalogueAccountingEntryCodeAdapter(
            @Value("${treasury.integrations.account-catalogue-url:http://localhost:8080}") String baseUrl,
            IJwtUtils jwtUtils) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Optional<String> resolveCode(Long accountingEntryId) {
        if (accountingEntryId == null) {
            return Optional.empty();
        }
        try {
            EntryApiResponse response = client.get()
                    .uri("/api/accountCatalogue/accounting/entries/{id}", accountingEntryId)
                    .header("Authorization", "Bearer " + jwtUtils.getToken())
                    .header("X-Tenant-ID", Optional.ofNullable(TenantContext.getTenantId()).orElse(""))
                    .retrieve()
                    .body(EntryApiResponse.class);
            if (response == null || !response.success() || response.data() == null) {
                return Optional.empty();
            }
            String code = response.data().code();
            if (code == null || code.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(code);
        } catch (Exception ex) {
            log.warn("No se pudo resolver código de asiento {}: {}", accountingEntryId, ex.getMessage());
            return Optional.empty();
        }
    }

    private record EntryApiResponse(boolean success, EntryData data) {}

    private record EntryData(String code) {}
}
