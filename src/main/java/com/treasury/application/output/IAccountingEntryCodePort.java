package com.treasury.application.output;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface IAccountingEntryCodePort {
    Optional<String> resolveCode(Long accountingEntryId);

    default Map<Long, String> resolveCodes(Set<Long> accountingEntryIds) {
        Map<Long, String> codes = new HashMap<>();
        if (accountingEntryIds == null) {
            return codes;
        }
        for (Long id : accountingEntryIds) {
            if (id != null) {
                resolveCode(id).ifPresent(code -> codes.put(id, code));
            }
        }
        return codes;
    }
}
