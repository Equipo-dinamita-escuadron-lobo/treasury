package com.treasury.infrastructure.adapters.input.rest.assembler;

import com.treasury.application.output.IPaymentVoucherQueryPersistencePort;
import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.ScheduleResponse;
import com.treasury.infrastructure.adapters.input.rest.mapper.IPaymentScheduleRestMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleResponseAssembler {
    private final IPaymentScheduleRestMapper mapper;
    private final IPaymentVoucherQueryPersistencePort vouchers;

    public ScheduleResponse toResponse(PaymentSchedule schedule) {
        ScheduleResponse base = mapper.toResponse(schedule);
        return enrich(base, resolveVoucherNumber(base.voucherId()));
    }

    public List<ScheduleResponse> toResponseList(List<PaymentSchedule> schedules) {
        List<ScheduleResponse> responses = mapper.toResponseList(schedules);
        Set<Long> voucherIds = responses.stream()
                .map(ScheduleResponse::voucherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> voucherNumbers = resolveVoucherNumbers(voucherIds);
        return responses.stream()
                .map(response -> enrich(response, response.voucherId() == null
                        ? null
                        : voucherNumbers.get(response.voucherId())))
                .toList();
    }

    private ScheduleResponse enrich(ScheduleResponse response, String voucherNumber) {
        return new ScheduleResponse(
                response.id(),
                response.enterpriseId(),
                response.executionDate(),
                response.paymentMethodId(),
                response.bankAccountId(),
                response.observations(),
                response.status(),
                response.voucherId(),
                voucherNumber,
                response.retryCount(),
                response.failureReason(),
                response.createdAt(),
                response.updatedAt(),
                response.version(),
                response.total(),
                response.details());
    }

    private String resolveVoucherNumber(Long voucherId) {
        if (voucherId == null) {
            return null;
        }
        return vouchers.findById(voucherId)
                .map(PaymentVoucher::getVoucherNumber)
                .orElse(null);
    }

    private Map<Long, String> resolveVoucherNumbers(Set<Long> voucherIds) {
        Map<Long, String> numbers = new HashMap<>();
        for (Long id : voucherIds) {
            vouchers.findById(id)
                    .map(PaymentVoucher::getVoucherNumber)
                    .ifPresent(number -> numbers.put(id, number));
        }
        return numbers;
    }
}
