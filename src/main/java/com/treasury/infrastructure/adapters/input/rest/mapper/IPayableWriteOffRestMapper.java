package com.treasury.infrastructure.adapters.input.rest.mapper;

import com.treasury.domain.model.PayableWriteOff;
import com.treasury.domain.model.PayableWriteOffDetail;
import com.treasury.domain.model.command.TreasuryCommands.WriteOff;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.WriteOffDetailResponse;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.WriteOffRequest;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.WriteOffResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IPayableWriteOffRestMapper {
    WriteOff toCommand(WriteOffRequest request);

    @Mapping(target = "accountingEntryCode", ignore = true)
    WriteOffResponse toResponse(PayableWriteOff writeOff);

    List<WriteOffResponse> toResponseList(List<PayableWriteOff> writeOffs);

    @Mapping(target = "invoiceReference", ignore = true)
    @Mapping(target = "originalAmount", ignore = true)
    @Mapping(target = "availableAmount", ignore = true)
    WriteOffDetailResponse toDetailResponse(PayableWriteOffDetail detail);
}
