package com.treasury.domain.model;

public record AccountCatalogueAccountSnapshot(String code, String description, Integer classification,
        boolean active) {
    public boolean isPayableLiability() {
        if (code != null && code.startsWith("11")) {
            return false;
        }
        return classification != null && (classification == 2 || classification == 3);
    }
}
