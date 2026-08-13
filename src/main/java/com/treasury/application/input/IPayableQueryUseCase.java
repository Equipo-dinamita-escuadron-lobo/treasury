package com.treasury.application.input;

import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.command.TreasuryCommands.AgingLine;
import com.treasury.domain.model.command.TreasuryCommands.SupplierStatement;
import java.time.LocalDate;
import java.util.List;

public interface IPayableQueryUseCase {
    List<SupplierInvoiceReplica> pending(String enterpriseId, Long supplierId);
    SupplierInvoiceReplica find(Long id, String enterpriseId);
    SupplierStatement statement(String enterpriseId, Long supplierId, LocalDate from, LocalDate to,
                                String invoiceReference, Boolean active);
    List<AgingLine> aging(String enterpriseId, LocalDate cutoff, Long supplierId, String accountCode,
                          String document);
}
