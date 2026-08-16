package com.treasury.infrastructure.adapters.input.rest.assembler;

import com.treasury.application.output.IAccountingEntryCodePort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.domain.model.PayableWriteOff;
import com.treasury.domain.model.PayableWriteOffDetail;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.WriteOffDetailResponse;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.WriteOffResponse;
import com.treasury.infrastructure.adapters.input.rest.mapper.IPayableWriteOffRestMapper;
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
public class WriteOffResponseAssembler {
    private final IPayableWriteOffRestMapper mapper;
    private final IAccountingEntryCodePort accountingEntryCodes;
    private final ISupplierInvoiceProviderPort invoices;

    public WriteOffResponse toResponse(PayableWriteOff writeOff) {
        WriteOffResponse base = mapper.toResponse(writeOff);
        Map<Long, String> references = resolveInvoiceReferences(writeOff.getDetails());
        return enrich(base, references);
    }

    public List<WriteOffResponse> toResponseList(List<PayableWriteOff> writeOffs) {
        List<WriteOffResponse> responses = mapper.toResponseList(writeOffs);
        Set<Long> invoiceIds = writeOffs.stream()
                .flatMap(writeOff -> writeOff.getDetails().stream())
                .map(PayableWriteOffDetail::getInvoiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> references = resolveInvoiceReferencesByIds(invoiceIds);
        Set<Long> entryIds = responses.stream()
                .map(WriteOffResponse::accountingEntryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> codes = accountingEntryCodes.resolveCodes(entryIds);
        return responses.stream()
                .map(response -> enrich(
                        response,
                        referencesForDetails(response.details(), references),
                        response.accountingEntryId() == null ? null : codes.get(response.accountingEntryId())))
                .toList();
    }

    private WriteOffResponse enrich(WriteOffResponse response, Map<Long, String> references) {
        String entryCode = response.accountingEntryId() == null
                ? null
                : accountingEntryCodes.resolveCode(response.accountingEntryId()).orElse(null);
        return enrich(response, referencesForDetails(response.details(), references), entryCode);
    }

    private WriteOffResponse enrich(
            WriteOffResponse response,
            List<WriteOffDetailResponse> details,
            String accountingEntryCode) {
        return new WriteOffResponse(
                response.id(),
                response.enterpriseId(),
                response.reason(),
                response.counterpartAccountId(),
                response.counterpartAccountCode(),
                response.total(),
                response.status(),
                response.accountingEntryId(),
                accountingEntryCode,
                response.createdAt(),
                response.version(),
                details);
    }

    private Map<Long, String> resolveInvoiceReferences(List<PayableWriteOffDetail> details) {
        Set<Long> ids = details.stream()
                .map(PayableWriteOffDetail::getInvoiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return resolveInvoiceReferencesByIds(ids);
    }

    private Map<Long, String> resolveInvoiceReferencesByIds(Set<Long> invoiceIds) {
        Map<Long, String> references = new HashMap<>();
        for (Long id : invoiceIds) {
            invoices.findById(id).ifPresent(invoice -> references.put(id, invoice.getReference()));
        }
        return references;
    }

    private List<WriteOffDetailResponse> referencesForDetails(
            List<WriteOffDetailResponse> details,
            Map<Long, String> references) {
        if (details == null) {
            return List.of();
        }
        return details.stream()
                .map(detail -> enrichDetail(detail, references.get(detail.invoiceId())))
                .toList();
    }

    private WriteOffDetailResponse enrichDetail(WriteOffDetailResponse detail, String invoiceReference) {
        String reference = detail.invoiceReference() != null ? detail.invoiceReference() : invoiceReference;
        Long supplierId = detail.supplierId();
        Long payableAccountId = detail.payableAccountId();
        String payableAccountCode = detail.payableAccountCode();
        java.math.BigDecimal originalAmount = detail.originalAmount();
        java.math.BigDecimal availableAmount = detail.availableAmount();
        if (detail.invoiceId() != null) {
            var invoice = invoices.findById(detail.invoiceId());
            if (invoice.isPresent()) {
                var replica = invoice.get();
                if (supplierId == null) {
                    supplierId = replica.getSupplierId();
                }
                if (payableAccountId == null) {
                    payableAccountId = replica.getPayableAccountId();
                }
                if (payableAccountCode == null) {
                    payableAccountCode = replica.getPayableAccountCode();
                }
                if (replica.getOriginalAmount() != null) {
                    originalAmount = replica.getOriginalAmount();
                }
                if (replica.getPendingAmount() != null) {
                    availableAmount = replica.available();
                }
                if (reference == null) {
                    reference = replica.getReference();
                }
            }
        }
        return new WriteOffDetailResponse(
                detail.id(),
                supplierId,
                detail.invoiceId(),
                reference,
                payableAccountId,
                payableAccountCode,
                detail.amount(),
                originalAmount,
                availableAmount);
    }
}
