package com.treasury.application.input;

import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.command.TreasuryCommands.DueDate;

public interface IPayableCommandUseCase {
    SupplierInvoiceReplica updateDueDate(Long id, String enterpriseId, DueDate command);
}
