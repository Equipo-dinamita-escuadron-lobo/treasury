package com.treasury.application.output;

import com.treasury.domain.model.DuePaymentSchedule;
import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.command.TreasuryCommands.ScheduleFilter;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IPaymentSchedulePersistencePort {
    PaymentSchedule save(PaymentSchedule schedule);
    Optional<PaymentSchedule> find(Long id);
    Optional<PaymentSchedule> findLocked(Long id);
    Optional<PaymentSchedule> findByVoucherId(Long voucherId);
    List<PaymentSchedule> search(ScheduleFilter filter);
    List<DuePaymentSchedule> findDue(LocalDate date);
    List<DuePaymentSchedule> findAbandoned(Instant before);
    List<DuePaymentSchedule> findWaitingAccounting(Instant before);
    void delete(PaymentSchedule schedule);
}
