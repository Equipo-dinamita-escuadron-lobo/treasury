package com.treasury.infrastructure.adapters.output.rest;

import com.treasury.application.output.IPaymentMethodProviderPort;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.PaymentMethodData;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import com.treasury.infrastructure.adapters.output.security.IJwtUtils;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class AccountCataloguePaymentMethodAdapter implements IPaymentMethodProviderPort {
    private final RestClient client;
    private final IJwtUtils jwtUtils;

    public AccountCataloguePaymentMethodAdapter(
            @Value("${treasury.integrations.account-catalogue-url:http://localhost:8080}") String baseUrl,
            IJwtUtils jwtUtils) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Optional<PaymentMethodData> findActive(Long paymentMethodId, String enterpriseId) {
        PaymentMethodResponse response = fetchPaymentMethod(paymentMethodId, enterpriseId);
        if (response == null || !Boolean.TRUE.equals(response.status())) {
            return Optional.empty();
        }
        Long accountingAccountId = response.accountingAccountId();
        if (accountingAccountId == null || !isActiveAccountingAccount(accountingAccountId, enterpriseId)) {
            return Optional.empty();
        }
        return Optional.of(new PaymentMethodData(
                response.id(),
                Boolean.TRUE.equals(response.requiresBankAccount()),
                accountingAccountId));
    }

    @Override
    public void validateForPayment(Long paymentMethodId, Long bankAccountId, String enterpriseId) {
        PaymentMethodResponse method = fetchPaymentMethod(paymentMethodId, enterpriseId);
        if (method == null) {
            throw badRequest("Metodo de pago inactivo o inexistente");
        }
        if (!Boolean.TRUE.equals(method.status())) {
            throw badRequest("Metodo de pago inactivo o inexistente");
        }
        Long accountingAccountId = method.accountingAccountId();
        if (accountingAccountId == null) {
            throw badRequest("El metodo de pago no tiene una cuenta contable configurada");
        }
        if (!isActiveAccountingAccount(accountingAccountId, enterpriseId)) {
            throw badRequest("La cuenta contable del metodo de pago esta inactiva o no pertenece a la empresa");
        }
        boolean requiresBank = Boolean.TRUE.equals(method.requiresBankAccount());
        if (requiresBank && bankAccountId == null) {
            throw badRequest("El metodo de pago exige cuenta bancaria");
        }
        if (!requiresBank && bankAccountId != null) {
            throw badRequest("El metodo de pago no admite cuenta bancaria");
        }
        if (bankAccountId != null && !isActiveBankAccount(bankAccountId, enterpriseId)) {
            throw badRequest("Cuenta bancaria inactiva o inexistente");
        }
    }

    @Override
    public boolean isActiveBankAccount(Long bankAccountId, String enterpriseId) {
        BankAccountResponse response = fetchBankAccount(bankAccountId, enterpriseId);
        if (response == null || !Boolean.TRUE.equals(response.status())) {
            return false;
        }
        Long accountingAccountId = response.accountingAccountId();
        return accountingAccountId != null && isActiveAccountingAccount(accountingAccountId, enterpriseId);
    }

    private PaymentMethodResponse fetchPaymentMethod(Long paymentMethodId, String enterpriseId) {
        try {
            return client.get()
                    .uri("/api/accountCatalogue/payment-methods/findById/{id}/{enterprise}", paymentMethodId, enterpriseId)
                    .header("Authorization", "Bearer " + jwtUtils.getToken())
                    .header("X-Tenant-ID", TenantContext.getTenantId())
                    .retrieve().body(PaymentMethodResponse.class);
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        }
    }

    private BankAccountResponse fetchBankAccount(Long bankAccountId, String enterpriseId) {
        try {
            return client.get()
                    .uri("/api/accountCatalogue/bank-accounts/findById/{id}/{enterprise}", bankAccountId, enterpriseId)
                    .header("Authorization", "Bearer " + jwtUtils.getToken())
                    .header("X-Tenant-ID", TenantContext.getTenantId())
                    .retrieve().body(BankAccountResponse.class);
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        }
    }

    private boolean isActiveAccountingAccount(Long accountingAccountId, String enterpriseId) {
        if (accountingAccountId == null || enterpriseId == null || enterpriseId.isBlank()) {
            return false;
        }
        try {
            AccountItem[] items = client.get()
                    .uri("/api/accountCatalogue/search/{enterpriseId}", enterpriseId)
                    .header("Authorization", "Bearer " + jwtUtils.getToken())
                    .header("X-Tenant-ID", Optional.ofNullable(TenantContext.getTenantId()).orElse(""))
                    .retrieve()
                    .body(AccountItem[].class);
            if (items == null) {
                return false;
            }
            return Arrays.stream(items)
                    .anyMatch(item -> accountingAccountId.equals(item.id()) && Boolean.TRUE.equals(item.status()));
        } catch (Exception ex) {
            return false;
        }
    }

    private TreasuryException badRequest(String message) {
        return new TreasuryException(TreasuryException.Type.BAD_REQUEST, message);
    }

    private record PaymentMethodResponse(Long id, Boolean status, Boolean requiresBankAccount, Long accountingAccountId) {}
    private record BankAccountResponse(Long id, Boolean status, Long accountingAccountId) {}
    private record AccountItem(Long id, Boolean status) {}
}
