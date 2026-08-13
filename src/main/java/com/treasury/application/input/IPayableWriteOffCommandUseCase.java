package com.treasury.application.input;

import com.treasury.domain.model.PayableWriteOff;
import com.treasury.domain.model.command.TreasuryCommands.AccountingResult;
import com.treasury.domain.model.command.TreasuryCommands.WriteOff;

public interface IPayableWriteOffCommandUseCase {
    PayableWriteOff create(WriteOff command);
    PayableWriteOff confirm(Long id);
    PayableWriteOff voidWriteOff(Long id);
    void applyAccountingResult(AccountingResult result);
}
