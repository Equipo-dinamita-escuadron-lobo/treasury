package com.treasury.application.input;

import com.treasury.domain.model.command.TreasuryCommands.PurchaseInvoiceEvent;

public interface IInvoiceSynchronizationUseCase {
    void synchronize(PurchaseInvoiceEvent event);
}
