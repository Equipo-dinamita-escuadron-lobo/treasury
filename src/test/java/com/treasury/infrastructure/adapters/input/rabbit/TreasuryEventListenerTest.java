package com.treasury.infrastructure.adapters.input.rabbit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.treasury.application.input.IInvoiceSynchronizationUseCase;
import com.treasury.application.input.IPayableWriteOffCommandUseCase;
import com.treasury.application.input.IPaymentScheduleExecutionUseCase;
import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.domain.model.command.TreasuryCommands.PurchaseInvoiceEvent;
import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.PurchaseInvoiceEnvelope;
import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.SourceDocument;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;

class TreasuryEventListenerTest {

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void usesAuthenticatedTenantAsCanonicalTenantForPersistence() {
        IInvoiceSynchronizationUseCase synchronization = mock(IInvoiceSynchronizationUseCase.class);
        TreasuryEventListener listener = new TreasuryEventListener(
                synchronization,
                mock(IPaymentVoucherCommandUseCase.class),
                mock(IPayableWriteOffCommandUseCase.class),
                mock(IPaymentScheduleExecutionUseCase.class));
        TenantContext.setTenantId("authenticated-tenant");
        var payload = new TreasuryRabbitDtos.PurchaseInvoiceEvent(
                10L, "FC-10", "enterprise-a", 20L,
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"),
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 9, 16),
                2205L, "2205", true, "declared-tenant");
        PurchaseInvoiceEnvelope envelope = new PurchaseInvoiceEnvelope(
                "event-10", "PURCHASE_INVOICE_CREATED", 1, Instant.now(),
                "declared-tenant", "enterprise-a", "event-10",
                new SourceDocument("PURCHASE_INVOICE", 10L), payload);

        listener.invoice(mock(Message.class), envelope);

        ArgumentCaptor<PurchaseInvoiceEvent> captured = ArgumentCaptor.forClass(PurchaseInvoiceEvent.class);
        verify(synchronization).synchronize(captured.capture());
        assertThat(captured.getValue().tenantId()).isEqualTo("authenticated-tenant");
        assertThat(captured.getValue().enterpriseId()).isEqualTo("enterprise-a");
        assertThat(captured.getValue().invoiceId()).isEqualTo(10L);
        assertThat(captured.getValue().supplierId()).isEqualTo(20L);
        assertThat(captured.getValue().pendingAmount()).isEqualByComparingTo("100000");
        assertThat(captured.getValue().active()).isTrue();
        assertThat(captured.getValue().payableAccountId()).isEqualTo(2205L);
        assertThat(captured.getValue().payableAccountCode()).isEqualTo("2205");
    }
}
