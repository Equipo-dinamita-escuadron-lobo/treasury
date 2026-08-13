package com.treasury.application.output;

import java.util.Optional;

public interface IAccountCodeResolverPort {
    Optional<String> resolveCode(Long accountId, String enterpriseId);
}
