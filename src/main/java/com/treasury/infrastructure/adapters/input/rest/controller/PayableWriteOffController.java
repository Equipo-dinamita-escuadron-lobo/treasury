package com.treasury.infrastructure.adapters.input.rest.controller;

import com.treasury.application.input.IPayableWriteOffCommandUseCase;
import com.treasury.application.input.IPayableWriteOffQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.WriteOffRequest;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.WriteOffResponse;
import com.treasury.infrastructure.adapters.input.rest.mapper.IPayableWriteOffRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/treasury/payable-write-offs") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('user_client','admin_client','super_client')")
public class PayableWriteOffController {
    private final IPayableWriteOffCommandUseCase commands;
    private final IPayableWriteOffQueryUseCase queries;
    private final IPayableWriteOffRestMapper mapper;
    @PostMapping public WriteOffResponse create(@Valid@RequestBody WriteOffRequest r){return mapper.toResponse(commands.create(mapper.toCommand(r)));}
    @GetMapping public List<WriteOffResponse> list(@RequestParam String enterpriseId){return mapper.toResponseList(queries.list(enterpriseId));}
    @GetMapping("/{id}")public WriteOffResponse find(@PathVariable Long id){return mapper.toResponse(queries.find(id));}
    @PostMapping("/{id}/confirm")public WriteOffResponse confirm(@PathVariable Long id){return mapper.toResponse(commands.confirm(id));}
    @PostMapping("/{id}/void")public WriteOffResponse voidWriteOff(@PathVariable Long id){return mapper.toResponse(commands.voidWriteOff(id));}
}
