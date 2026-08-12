package com.treasury.infrastructure.adapters.output.rest;

import com.treasury.application.output.IPaymentMethodProviderPort;
import com.treasury.domain.model.PaymentMethodData;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import com.treasury.infrastructure.adapters.output.security.ServiceJwtTokenProvider;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;

@Component
public class AccountCataloguePaymentMethodAdapter implements IPaymentMethodProviderPort {
    private final RestClient client;
    private final ServiceJwtTokenProvider serviceTokens;

    public AccountCataloguePaymentMethodAdapter(
            @Value("${treasury.integrations.account-catalogue-url:http://localhost:8080}") String baseUrl,
            ServiceJwtTokenProvider serviceTokens) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.serviceTokens = serviceTokens;
    }

    @Override
    public Optional<PaymentMethodData> findActive(Long paymentMethodId, String enterpriseId) {
        try {
            PaymentMethodResponse response = client.get()
                    .uri("/api/accountCatalogue/payment-methods/findById/{id}/{enterprise}", paymentMethodId, enterpriseId)
                    .header("Authorization", bearer())
                    .header("X-Tenant-ID", TenantContext.getTenantId())
                    .retrieve().body(PaymentMethodResponse.class);
            if (response == null || !Boolean.TRUE.equals(response.status())) return Optional.empty();
            return Optional.of(new PaymentMethodData(response.id(), Boolean.TRUE.equals(response.requiresBankAccount())));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isActiveBankAccount(Long bankAccountId, String enterpriseId) {
        try {
            BankAccountResponse response = client.get()
                    .uri("/api/accountCatalogue/bank-accounts/findById/{id}/{enterprise}", bankAccountId, enterpriseId)
                    .header("Authorization", bearer())
                    .header("X-Tenant-ID", TenantContext.getTenantId())
                    .retrieve().body(BankAccountResponse.class);
            return response != null && Boolean.TRUE.equals(response.status());
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        }
    }

    private String bearer() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) return "Bearer " + jwt.getToken().getTokenValue();
        return serviceTokens.bearerToken();
    }

    private record PaymentMethodResponse(Long id, Boolean status, Boolean requiresBankAccount) {}
    private record BankAccountResponse(Long id, Boolean status) {}
}
