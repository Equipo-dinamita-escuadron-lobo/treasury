package com.treasury.infrastructure.adapters.input.rest.assembler;

import com.treasury.application.output.IAccountingEntryCodePort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.domain.model.PayableWriteOff;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.WriteOffStatus;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.WriteOffDetailResponse;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.WriteOffResponse;
import com.treasury.infrastructure.adapters.input.rest.mapper.IPayableWriteOffRestMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WriteOffResponseAssemblerTest {
    @Mock
    IPayableWriteOffRestMapper mapper;
    @Mock
    IAccountingEntryCodePort accountingEntryCodes;
    @Mock
    ISupplierInvoiceProviderPort invoices;
    @Mock
    PayableWriteOff writeOff;

    WriteOffResponseAssembler assembler;

    @BeforeEach
    void setup() {
        assembler = new WriteOffResponseAssembler(mapper, accountingEntryCodes, invoices);
    }

    @Test
    void enrichesAccountingEntryCodeAndInvoiceReference() {
        WriteOffResponse base = sampleResponse(37L, null, Instant.parse("2026-08-14T10:00:00Z"));
        SupplierInvoiceReplica invoice = new SupplierInvoiceReplica();
        invoice.setReference("678151694");
        invoice.setPendingAmount(BigDecimal.ZERO);
        when(mapper.toResponse(writeOff)).thenReturn(base);
        when(accountingEntryCodes.resolveCode(37L)).thenReturn(Optional.of("AE-PWO-12"));
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice));

        WriteOffResponse response = assembler.toResponse(writeOff);

        assertThat(response.accountingEntryCode()).isEqualTo("AE-PWO-12");
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-08-14T10:00:00Z"));
        assertThat(response.details().get(0).invoiceReference()).isEqualTo("678151694");
    }

    @Test
    void enrichesSupplierAndBalancesFromInvoiceReplica() {
        WriteOffResponse base = sampleResponse(37L, null, Instant.parse("2026-08-14T10:00:00Z"));
        SupplierInvoiceReplica invoice = new SupplierInvoiceReplica();
        invoice.setReference("678151694");
        invoice.setSupplierId(7L);
        invoice.setOriginalAmount(BigDecimal.valueOf(5000));
        invoice.setPendingAmount(BigDecimal.valueOf(3000));
        invoice.setReservedAmount(BigDecimal.ZERO);
        when(mapper.toResponse(writeOff)).thenReturn(base);
        when(accountingEntryCodes.resolveCode(37L)).thenReturn(Optional.of("AE-PWO-12"));
        when(invoices.findById(11L)).thenReturn(Optional.of(invoice));

        WriteOffResponse response = assembler.toResponse(writeOff);

        WriteOffDetailResponse detail = response.details().get(0);
        assertThat(detail.supplierId()).isEqualTo(7L);
        assertThat(detail.originalAmount()).isEqualByComparingTo("5000");
        assertThat(detail.availableAmount()).isEqualByComparingTo("3000");
    }

    @Test
    void resolvesCodesInBatchForList() {
        WriteOffResponse posted = sampleResponse(37L, null, Instant.parse("2026-08-14T10:00:00Z"));
        WriteOffResponse draft = sampleResponse(null, null, Instant.parse("2026-08-13T10:00:00Z"));
        when(mapper.toResponseList(List.of(writeOff, writeOff))).thenReturn(List.of(posted, draft));
        when(accountingEntryCodes.resolveCodes(java.util.Set.of(37L)))
                .thenReturn(java.util.Map.of(37L, "AE-PWO-12"));
        when(invoices.findById(11L)).thenReturn(Optional.empty());

        List<WriteOffResponse> responses = assembler.toResponseList(List.of(writeOff, writeOff));

        assertThat(responses.get(0).accountingEntryCode()).isEqualTo("AE-PWO-12");
        assertThat(responses.get(1).accountingEntryCode()).isNull();
    }

    private WriteOffResponse sampleResponse(Long accountingEntryId, String accountingEntryCode, Instant createdAt) {
        return new WriteOffResponse(
                12L,
                "enterprise-a",
                "Condonación",
                4295L,
                "429501",
                BigDecimal.valueOf(100),
                WriteOffStatus.POSTED,
                accountingEntryId,
                accountingEntryCode,
                createdAt,
                0L,
                List.of(new WriteOffDetailResponse(
                        1L,
                        7L,
                        11L,
                        null,
                        2205L,
                        "220501",
                        BigDecimal.valueOf(100),
                        null,
                        null)));
    }
}
