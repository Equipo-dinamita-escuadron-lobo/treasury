package com.treasury.infrastructure.adapters.input.rest.controller;

import com.treasury.application.input.IPaymentScheduleCommandUseCase;
import com.treasury.application.input.IPaymentScheduleExecutionUseCase;
import com.treasury.application.input.IPaymentScheduleQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.domain.model.*;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.ScheduleRequest;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.ScheduleResponse;
import com.treasury.infrastructure.adapters.input.rest.mapper.IPaymentScheduleRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/treasury/payment-schedules") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Estudiante','Profesor','Administrador')")
public class PaymentScheduleController {
    private final IPaymentScheduleCommandUseCase commands;
    private final IPaymentScheduleQueryUseCase queries;
    private final IPaymentScheduleExecutionUseCase executions;
    private final IPaymentScheduleRestMapper mapper;
    @PostMapping public ScheduleResponse create(@Valid@RequestBody ScheduleRequest r){return mapper.toResponse(commands.create(mapper.toCommand(r)));}
    @PutMapping("/{id}")public ScheduleResponse update(@PathVariable Long id,@Valid@RequestBody ScheduleRequest r){return mapper.toResponse(commands.update(id,mapper.toCommand(r)));}
    @DeleteMapping("/{id}")public void delete(@PathVariable Long id){commands.delete(id);}
    @GetMapping("/{id}")public ScheduleResponse find(@PathVariable Long id){return mapper.toResponse(queries.find(id));}
    @GetMapping public List<ScheduleResponse> list(@RequestParam String enterpriseId,@RequestParam(required=false)PaymentScheduleStatus status,@RequestParam(required=false)LocalDate from,@RequestParam(required=false)LocalDate to){return mapper.toResponseList(queries.list(new ScheduleFilter(enterpriseId,status,from,to)));}
    @PostMapping("/{id}/cancel")public ScheduleResponse cancel(@PathVariable Long id){return mapper.toResponse(commands.cancel(id));}
    @PostMapping("/{id}/execute")public ScheduleResponse execute(@PathVariable Long id){return mapper.toResponse(executions.execute(id));}
    @PostMapping("/{id}/retry")public ScheduleResponse retry(@PathVariable Long id){return mapper.toResponse(executions.execute(id));}
}
