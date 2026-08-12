package com.treasury.application.input;

import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.command.TreasuryCommands.Schedule;

public interface IPaymentScheduleCommandUseCase {
    PaymentSchedule create(Schedule command);
    PaymentSchedule update(Long id, Schedule command);
    void delete(Long id);
    PaymentSchedule cancel(Long id);
}
