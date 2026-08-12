package com.treasury.application.input;

import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.command.TreasuryCommands.ScheduleFilter;
import java.util.List;

public interface IPaymentScheduleQueryUseCase {
    PaymentSchedule find(Long id);
    List<PaymentSchedule> list(ScheduleFilter filter);
}
