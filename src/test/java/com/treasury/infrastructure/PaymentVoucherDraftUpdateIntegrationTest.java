package com.treasury.infrastructure;

import com.treasury.application.input.IInvoiceSynchronizationUseCase;
import com.treasury.application.input.IPayableQueryUseCase;
import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.application.output.IPaymentMethodProviderPort;
import com.treasury.domain.model.PaymentMethodData;
import com.treasury.domain.model.PaymentVoucherStatus;
import com.treasury.domain.model.command.TreasuryCommands.Detail;
import com.treasury.domain.model.command.TreasuryCommands.PurchaseInvoiceEvent;
import com.treasury.domain.model.command.TreasuryCommands.Voucher;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * L-01 / B-25: updating a DRAFT voucher while keeping the same invoice/obligation
 * must not violate uk_voucher_invoice (delete-then-insert ordering).
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentVoucherDraftUpdateIntegrationTest {

    private static final String TENANT = "draft-update-tenant";
    private static final String ENTERPRISE = "enterprise-draft-update";

    @Autowired IInvoiceSynchronizationUseCase synchronization;
    @Autowired IPayableQueryUseCase payables;
    @Autowired IPaymentVoucherCommandUseCase voucherCommands;
    @Autowired IPaymentVoucherQueryUseCase voucherQueries;
    @MockBean IPaymentMethodProviderPort paymentMethods;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT);
        when(paymentMethods.findActive(17L, ENTERPRISE))
                .thenReturn(Optional.of(new PaymentMethodData(17L, false)));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void updatingDraftVoucherWithSameObligationSucceeds() {
        synchronization.synchronize(new PurchaseInvoiceEvent(
                "draft-update-purchase",
                "PURCHASE_INVOICE_CREATED",
                8001L,
                "FC-8001",
                ENTERPRISE,
                601L,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                2205L,
                "2205",
                true,
                TENANT));

        var invoice = payables.pending(ENTERPRISE, 601L).get(0);

        var created = voucherCommands.create(new Voucher(
                ENTERPRISE,
                LocalDate.now(),
                17L,
                null,
                "borrador inicial",
                List.of(new Detail(601L, invoice.getId(), new BigDecimal("30.00")))));

        assertThat(created.getStatus()).isEqualTo(PaymentVoucherStatus.DRAFT);
        assertThat(created.getTotal()).isEqualByComparingTo("30.00");

        var updated = voucherCommands.update(
                created.getId(),
                new Voucher(
                        ENTERPRISE,
                        LocalDate.now(),
                        17L,
                        null,
                        "borrador modificado misma obligación",
                        List.of(new Detail(601L, invoice.getId(), new BigDecimal("45.00")))));

        assertThat(updated.getStatus()).isEqualTo(PaymentVoucherStatus.DRAFT);
        assertThat(updated.getTotal()).isEqualByComparingTo("45.00");
        assertThat(updated.getDetails()).hasSize(1);
        assertThat(updated.getDetails().get(0).getInvoiceId()).isEqualTo(invoice.getId());
        assertThat(updated.getDetails().get(0).getAmountPaid()).isEqualByComparingTo("45.00");
        assertThat(updated.getObservations()).isEqualTo("borrador modificado misma obligación");

        var reloaded = voucherQueries.find(created.getId(), ENTERPRISE);
        assertThat(reloaded.getTotal()).isEqualByComparingTo("45.00");
        assertThat(reloaded.getDetails()).hasSize(1);
        assertThat(reloaded.getDetails().get(0).getInvoiceId()).isEqualTo(invoice.getId());
        assertThat(reloaded.getDetails().get(0).getAmountPaid()).isEqualByComparingTo("45.00");
    }
}
