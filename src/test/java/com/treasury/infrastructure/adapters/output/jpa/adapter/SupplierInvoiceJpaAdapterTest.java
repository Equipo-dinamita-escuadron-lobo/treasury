package com.treasury.infrastructure.adapters.output.jpa.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.treasury.infrastructure.adapters.output.jpa.mapper.ISupplierInvoicePersistenceMapper;
import com.treasury.infrastructure.adapters.output.jpa.repository.ISupplierInvoiceRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SupplierInvoiceJpaAdapterTest {

    @Test
    void pendingQueryRequiresPositiveBalanceAndActiveReplica() {
        ISupplierInvoiceRepository repository = mock(ISupplierInvoiceRepository.class);
        ISupplierInvoicePersistenceMapper mapper = mock(ISupplierInvoicePersistenceMapper.class);
        when(repository.findByEnterpriseIdAndPendingAmountGreaterThanAndActiveTrue(
                "enterprise-a", BigDecimal.ZERO)).thenReturn(List.of());
        when(mapper.toDomainList(List.of())).thenReturn(List.of());
        SupplierInvoiceJpaAdapter adapter = new SupplierInvoiceJpaAdapter(repository, mapper);

        assertThat(adapter.findPending("enterprise-a", null)).isEmpty();

        verify(repository).findByEnterpriseIdAndPendingAmountGreaterThanAndActiveTrue(
                "enterprise-a", BigDecimal.ZERO);
    }
}
