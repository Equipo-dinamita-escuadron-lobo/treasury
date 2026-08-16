package com.treasury.application.service;

import com.treasury.application.output.IPaymentSchedulePersistencePort;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.PaymentScheduleStatus;
import com.treasury.domain.model.command.TreasuryCommands.ScheduleFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PaymentScheduleBalanceGuard {
    public static final String ACTIVE_SCHEDULE_BLOCKS_BALANCE_CHANGE =
            "La obligación tiene una programación de pago activa. Cancela primero la programación antes de modificar su saldo.";
    public static final String ACTIVE_SCHEDULE_BLOCKS_NEW_SCHEDULE =
            "La obligación ya tiene una programación de pago activa.";

    private final IPaymentSchedulePersistencePort schedules;

    public void assertBalanceChangeAllowed(String enterpriseId, Collection<Long> invoiceIds) {
        assertBalanceChangeAllowed(enterpriseId, invoiceIds, null);
    }

    public void assertBalanceChangeAllowed(String enterpriseId, Collection<Long> invoiceIds, Long excludeScheduleId) {
        if (hasBlockingSchedule(enterpriseId, invoiceIds, excludeScheduleId)) {
            conflict(ACTIVE_SCHEDULE_BLOCKS_BALANCE_CHANGE);
        }
    }

    public void assertNewScheduleAllowed(String enterpriseId, Collection<Long> invoiceIds, Long excludeScheduleId) {
        if (hasBlockingSchedule(enterpriseId, invoiceIds, excludeScheduleId)) {
            conflict(ACTIVE_SCHEDULE_BLOCKS_NEW_SCHEDULE);
        }
    }

    public boolean isScheduleOwnedVoucher(Long voucherId) {
        return schedules.findByVoucherId(voucherId)
                .filter(schedule -> schedule.getStatus() == PaymentScheduleStatus.PROCESSING
                        || schedule.getStatus() == PaymentScheduleStatus.WAITING_ACCOUNTING
                        || schedule.getStatus() == PaymentScheduleStatus.FAILED)
                .isPresent();
    }

    private boolean hasBlockingSchedule(String enterpriseId, Collection<Long> invoiceIds, Long excludeScheduleId) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return false;
        }
        Set<Long> targets = new HashSet<>(invoiceIds);
        return schedules.search(new ScheduleFilter(enterpriseId, null, null, null)).stream()
                .filter(schedule -> schedule.getStatus() == PaymentScheduleStatus.SCHEDULED
                        || schedule.getStatus() == PaymentScheduleStatus.FAILED)
                .filter(schedule -> excludeScheduleId == null || !Objects.equals(excludeScheduleId, schedule.getId()))
                .anyMatch(schedule -> schedule.getDetails().stream()
                        .anyMatch(detail -> !detail.isCanceled() && targets.contains(detail.getInvoiceId())));
    }

    private void conflict(String message) {
        throw new TreasuryException(TreasuryException.Type.CONFLICT, message);
    }
}
