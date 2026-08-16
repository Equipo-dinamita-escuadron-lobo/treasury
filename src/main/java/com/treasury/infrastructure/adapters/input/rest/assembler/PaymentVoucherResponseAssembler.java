package com.treasury.infrastructure.adapters.input.rest.assembler;

import com.treasury.application.output.IAccountingEntryCodePort;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.VoucherResponse;
import com.treasury.infrastructure.adapters.input.rest.mapper.IPaymentVoucherRestMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentVoucherResponseAssembler {
    private final IPaymentVoucherRestMapper mapper;
    private final IAccountingEntryCodePort accountingEntryCodes;

    public VoucherResponse toResponse(PaymentVoucher voucher) {
        return enrich(mapper.toResponse(voucher));
    }

    public List<VoucherResponse> toResponseList(List<PaymentVoucher> vouchers) {
        List<VoucherResponse> responses = mapper.toResponseList(vouchers);
        Set<Long> entryIds = responses.stream()
                .map(VoucherResponse::accountingEntryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> codes = accountingEntryCodes.resolveCodes(entryIds);
        return responses.stream()
                .map(response -> withAccountingEntryCode(
                        response,
                        response.accountingEntryId() == null ? null : codes.get(response.accountingEntryId())))
                .toList();
    }

    private VoucherResponse enrich(VoucherResponse response) {
        if (response.accountingEntryId() == null) {
            return withAccountingEntryCode(response, null);
        }
        String code = accountingEntryCodes.resolveCode(response.accountingEntryId()).orElse(null);
        return withAccountingEntryCode(response, code);
    }

    private VoucherResponse withAccountingEntryCode(VoucherResponse response, String accountingEntryCode) {
        return new VoucherResponse(
                response.id(),
                response.voucherNumber(),
                response.enterpriseId(),
                response.issueDate(),
                response.status(),
                response.paymentMethodId(),
                response.bankAccountId(),
                response.total(),
                response.observations(),
                response.accountingEntryId(),
                accountingEntryCode,
                response.failureReason(),
                response.voidReason(),
                response.createdAt(),
                response.updatedAt(),
                response.version(),
                response.details());
    }
}
