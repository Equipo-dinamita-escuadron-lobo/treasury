package com.treasury.infrastructure.adapters.input.rabbit;

import com.treasury.application.input.IInvoiceSynchronizationUseCase;
import com.treasury.application.input.IPayableWriteOffCommandUseCase;
import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentScheduleExecutionUseCase;
import com.treasury.domain.model.command.TreasuryCommands;
import com.treasury.infrastructure.adapters.config.RabbitConfig;
import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.AccountingResultEnvelope;
import com.treasury.infrastructure.adapters.input.rabbit.TreasuryRabbitDtos.PurchaseInvoiceEnvelope;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TreasuryEventListener {
    private final IInvoiceSynchronizationUseCase invoiceSynchronization;
    private final IPaymentVoucherCommandUseCase vouchers;
    private final IPayableWriteOffCommandUseCase writeOffs;
    private final IPaymentScheduleExecutionUseCase schedules;

    @RabbitListener(queues = RabbitConfig.PURCHASE_QUEUE,
            containerFactory = "treasuryRabbitListenerContainerFactory")
    public void invoice(Message message, PurchaseInvoiceEnvelope envelope) {
        var event = envelope.payload();
        String authenticatedTenant = TenantContext.getTenantId();
        if (authenticatedTenant == null || authenticatedTenant.isBlank()) {
            throw new IllegalStateException("No existe tenant autenticado para procesar la factura de compra");
        }
        invoiceSynchronization.synchronize(new TreasuryCommands.PurchaseInvoiceEvent(
                envelope.eventId(), envelope.eventType(), event.invoiceId(), event.reference(),
                event.enterpriseId(), event.supplierId(), event.originalAmount(), event.paidAmount(),
                event.pendingAmount(), event.issueDate(), event.dueDate(), event.payableAccountId(),
                event.payableAccountCode(), event.active(), authenticatedTenant));
    }

    @RabbitListener(queues = RabbitConfig.RESULT_QUEUE,
            containerFactory = "treasuryRabbitListenerContainerFactory")
    public void accounting(Message message, AccountingResultEnvelope envelope) {
        var event = envelope.payload();
        var result = new TreasuryCommands.AccountingResult(
                event.eventId(), event.sourceEventId(), event.operation(), event.documentType(), event.documentId(), event.accepted(),
                event.accountingEntryId(), event.reason(), event.tenantId());
        vouchers.applyAccountingResult(result);
        writeOffs.applyAccountingResult(result);
        schedules.applyAccountingResult(result);
    }
}
