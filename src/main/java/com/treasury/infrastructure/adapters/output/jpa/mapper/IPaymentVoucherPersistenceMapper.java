package com.treasury.infrastructure.adapters.output.jpa.mapper;

import com.treasury.domain.model.PaymentVoucher;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IPaymentVoucherPersistenceMapper {
    PaymentVoucher toDomain(PaymentVoucherEntity entity);
    PaymentVoucherEntity toEntity(PaymentVoucher domain);

    @AfterMapping
    default void linkDetails(@MappingTarget PaymentVoucherEntity entity) {
        if (entity.getDetails() != null) {
            entity.getDetails().forEach(detail -> detail.setPaymentVoucher(entity));
        }
    }
}
