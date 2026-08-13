package com.treasury.infrastructure.adapters.input.rest.mapper;

import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.PayableResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IPayableRestMapper {
    @Mapping(target = "availableAmount", expression = "java(invoice.available())")
    PayableResponse toResponse(SupplierInvoiceReplica invoice);
    List<PayableResponse> toResponseList(List<SupplierInvoiceReplica> invoices);
}
