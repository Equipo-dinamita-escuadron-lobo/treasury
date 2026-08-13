package com.treasury.application.input;

import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.command.TreasuryCommands.AccountingResult;
import java.time.LocalDate;
import java.time.Instant;

public interface IPaymentScheduleExecutionUseCase {
    PaymentSchedule execute(Long id);
    void executeDue(LocalDate date);
    void applyAccountingResult(AccountingResult result);
    void recoverAbandoned(Instant before);
}
