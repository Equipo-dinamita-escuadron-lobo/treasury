package com.treasury.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountCatalogueAccountSnapshotTest {

    @Test
    void rejectsCashAccountCodesEvenWithLiabilityClassification() {
        assertFalse(new AccountCatalogueAccountSnapshot("1105", "Caja", 2, true).isPayableLiability());
    }

    @Test
    void acceptsCurrentLiabilityAccounts() {
        assertTrue(new AccountCatalogueAccountSnapshot("220501", "Proveedores", 2, true).isPayableLiability());
    }
}
