package com.treasury.infrastructure.adapters.input.rest.assembler;

import com.treasury.application.output.IAccountingEntryCodePort;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.PaymentVoucherStatus;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.VoucherResponse;
import com.treasury.infrastructure.adapters.input.rest.mapper.IPaymentVoucherRestMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentVoucherResponseAssemblerTest {
    @Mock
    IPaymentVoucherRestMapper mapper;
    @Mock
    IAccountingEntryCodePort accountingEntryCodes;
  @Mock
    PaymentVoucher voucher;

    PaymentVoucherResponseAssembler assembler;

    @BeforeEach
    void setup() {
        assembler = new PaymentVoucherResponseAssembler(mapper, accountingEntryCodes);
    }

    @Test
    void enrichesAccountingEntryCodeFromAccounting() {
        VoucherResponse base = sampleResponse(37L, null);
        when(mapper.toResponse(voucher)).thenReturn(base);
        when(accountingEntryCodes.resolveCode(37L)).thenReturn(java.util.Optional.of("AE-PV-42"));

        VoucherResponse response = assembler.toResponse(voucher);

        assertThat(response.accountingEntryId()).isEqualTo(37L);
        assertThat(response.accountingEntryCode()).isEqualTo("AE-PV-42");
    }

    @Test
    void leavesAccountingEntryCodeNullWhenNoEntry() {
        VoucherResponse base = sampleResponse(null, null);
        when(mapper.toResponse(voucher)).thenReturn(base);

        VoucherResponse response = assembler.toResponse(voucher);

        assertThat(response.accountingEntryId()).isNull();
        assertThat(response.accountingEntryCode()).isNull();
    }

    @Test
    void resolvesCodesInBatchForList() {
        VoucherResponse posted = sampleResponse(37L, null);
        VoucherResponse draft = sampleResponse(null, null);
        when(mapper.toResponseList(List.of(voucher, voucher))).thenReturn(List.of(posted, draft));
        when(accountingEntryCodes.resolveCodes(java.util.Set.of(37L)))
                .thenReturn(java.util.Map.of(37L, "AE-PV-42"));

        List<VoucherResponse> responses = assembler.toResponseList(List.of(voucher, voucher));

        assertThat(responses.get(0).accountingEntryCode()).isEqualTo("AE-PV-42");
        assertThat(responses.get(1).accountingEntryCode()).isNull();
    }

    private VoucherResponse sampleResponse(Long accountingEntryId, String accountingEntryCode) {
        return new VoucherResponse(
                99L,
                "CE-FB04BD89",
                "enterprise-a",
                LocalDate.of(2026, 8, 14),
                PaymentVoucherStatus.POSTED,
                1L,
                2L,
                BigDecimal.valueOf(22323),
                "obs",
                accountingEntryId,
                accountingEntryCode,
                null,
                null,
                java.time.Instant.parse("2026-08-14T10:00:00Z"),
                java.time.Instant.parse("2026-08-14T10:05:00Z"),
                0L,
                List.of());
    }
}
