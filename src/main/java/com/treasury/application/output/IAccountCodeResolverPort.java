package com.treasury.application.output;

import com.treasury.domain.model.AccountCatalogueAccountSnapshot;
import java.util.Optional;

public interface IAccountCodeResolverPort {
    Optional<String> resolveCode(Long accountId, String enterpriseId);

    Optional<AccountCatalogueAccountSnapshot> resolveAccount(Long accountId, String enterpriseId);
}
