package com.treasury.infrastructure.adapters.input.rest.mapper;

import org.mapstruct.Mapper;

import com.treasury.domain.model.Treasury;
import com.treasury.infrastructure.adapters.input.rest.dto.request.TreasuryDtoRequest;
import com.treasury.infrastructure.adapters.input.rest.dto.response.TreasuryDtoResponse;

@Mapper(componentModel = "spring")
public interface ITreasuryRestMapper {
    Treasury toDomain(TreasuryDtoRequest treasuryDtoRequest);
    TreasuryDtoResponse toDtoResponse(Treasury treasury);
}
