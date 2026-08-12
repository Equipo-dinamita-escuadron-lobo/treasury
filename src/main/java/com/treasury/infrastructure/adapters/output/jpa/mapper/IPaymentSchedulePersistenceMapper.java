package com.treasury.infrastructure.adapters.output.jpa.mapper;

import com.treasury.domain.model.PaymentSchedule;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentScheduleEntity;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IPaymentSchedulePersistenceMapper {
    PaymentSchedule toDomain(PaymentScheduleEntity entity);
    List<PaymentSchedule> toDomainList(List<PaymentScheduleEntity> entities);
    PaymentScheduleEntity toEntity(PaymentSchedule domain);

    @AfterMapping
    default void linkDetails(@MappingTarget PaymentScheduleEntity entity) {
        if (entity.getDetails() != null) {
            entity.getDetails().forEach(detail -> detail.setSchedule(entity));
        }
    }
}
