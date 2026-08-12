package com.treasury.infrastructure.adapters.input.rest.mapper;

import com.treasury.domain.model.PaymentSchedule;
import com.treasury.domain.model.command.TreasuryCommands.Schedule;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.ScheduleRequest;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.ScheduleResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IPaymentScheduleRestMapper {
    Schedule toCommand(ScheduleRequest request);
    ScheduleResponse toResponse(PaymentSchedule schedule);
    List<ScheduleResponse> toResponseList(List<PaymentSchedule> schedules);
}
