package com.treasury.infrastructure.adapters.output.jpa.mapper;

import org.mapstruct.Mapper;

import com.treasury.domain.model.Treasury;
import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface TreasuryEntityMapper {
    Treasury toDomain(TreasuryEntity treasuryEntity);
    TreasuryEntity toEntity(Treasury treasury);
}
