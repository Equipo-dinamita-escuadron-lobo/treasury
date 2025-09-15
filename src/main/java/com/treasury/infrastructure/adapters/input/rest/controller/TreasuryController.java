package com.treasury.infrastructure.adapters.input.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.treasury.application.ports.input.ITreasuryCommandPort;
import com.treasury.application.ports.input.ITreasuryQueryPort;
import com.treasury.domain.model.Treasury;
import com.treasury.infrastructure.adapters.input.rest.dto.ResponseDto;
import com.treasury.infrastructure.adapters.input.rest.dto.request.TreasuryDtoRequest;
import com.treasury.infrastructure.adapters.input.rest.dto.request.TransferDtoRequest;
import com.treasury.infrastructure.adapters.input.rest.dto.response.TreasuryDtoResponse;
import com.treasury.infrastructure.adapters.input.rest.mapper.ITreasuryRestMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/treasury")
public class TreasuryController {

    private final ITreasuryCommandPort treasuryCommandPort;
    private final ITreasuryQueryPort treasuryQueryPort;
    private final ITreasuryRestMapper treasuryRestMapper;

    @PostMapping("/account")
    public ResponseEntity<ResponseDto<TreasuryDtoResponse>> createAccount(@RequestBody TreasuryDtoRequest treasuryDtoRequest) {
        Treasury treasury = treasuryRestMapper.toDomain(treasuryDtoRequest);
        Treasury createdAccount = treasuryCommandPort.createAccount(treasury);
        TreasuryDtoResponse treasuryDtoResponse = treasuryRestMapper.toDtoResponse(createdAccount);
        return ResponseDto.<TreasuryDtoResponse>builder()
                .data(treasuryDtoResponse)
                .status(201)
                .message("Account created successfully").build().of();
    }

    @PutMapping("/deposit")
    public ResponseEntity<ResponseDto<TreasuryDtoResponse>> deposit(@RequestBody TreasuryDtoRequest treasuryDtoRequest) {
        Treasury treasury = treasuryRestMapper.toDomain(treasuryDtoRequest);
        Treasury updatedAccount = treasuryCommandPort.deposit(treasury);
        TreasuryDtoResponse treasuryDtoResponse = treasuryRestMapper.toDtoResponse(updatedAccount);
        return ResponseDto.<TreasuryDtoResponse>builder()
                .data(treasuryDtoResponse)
                .status(200)
                .message("Deposit completed successfully").build().of();
    }

    @PutMapping("/withdraw")
    public ResponseEntity<ResponseDto<TreasuryDtoResponse>> withdraw(@RequestBody TreasuryDtoRequest treasuryDtoRequest) {
        Treasury treasury = treasuryRestMapper.toDomain(treasuryDtoRequest);
        Treasury updatedAccount = treasuryCommandPort.withdraw(treasury);
        TreasuryDtoResponse treasuryDtoResponse = treasuryRestMapper.toDtoResponse(updatedAccount);
        return ResponseDto.<TreasuryDtoResponse>builder()
                .data(treasuryDtoResponse)
                .status(200)
                .message("Withdrawal completed successfully").build().of();
    }

    @PutMapping("/transfer")
    public ResponseEntity<ResponseDto<TreasuryDtoResponse>> transfer(@RequestBody TransferDtoRequest transferDtoRequest) {
        Treasury sourceAccount = treasuryCommandPort.transfer(
            transferDtoRequest.getSourceAccountId(),
            transferDtoRequest.getDestinationAccountId(),
            transferDtoRequest.getAmount()
        );
        TreasuryDtoResponse treasuryDtoResponse = treasuryRestMapper.toDtoResponse(sourceAccount);
        return ResponseDto.<TreasuryDtoResponse>builder()
                .data(treasuryDtoResponse)
                .status(200)
                .message("Transfer completed successfully").build().of();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<TreasuryDtoResponse>> getAccountById(@PathVariable Long id) {
        Treasury treasury = treasuryQueryPort.findById(id);
        TreasuryDtoResponse treasuryDtoResponse = treasuryRestMapper.toDtoResponse(treasury);
        return ResponseDto.<TreasuryDtoResponse>builder()
                .data(treasuryDtoResponse)
                .status(200)
                .message("Account retrieved successfully").build().of();
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ResponseDto<TreasuryDtoResponse>> getAccountByNumber(@PathVariable String accountNumber) {
        Treasury treasury = treasuryQueryPort.findByAccountNumber(accountNumber);
        TreasuryDtoResponse treasuryDtoResponse = treasuryRestMapper.toDtoResponse(treasury);
        return ResponseDto.<TreasuryDtoResponse>builder()
                .data(treasuryDtoResponse)
                .status(200)
                .message("Account retrieved successfully").build().of();
    }

    @PutMapping("/activate/{id}")
    public ResponseEntity<ResponseDto<String>> activateAccount(@PathVariable Long id) {
        treasuryCommandPort.activate(id);
        return ResponseDto.<String>builder()
                .data("Account activated")
                .status(200)
                .message("Account activated successfully").build().of();
    }

    @PutMapping("/inactivate/{id}")
    public ResponseEntity<ResponseDto<String>> inactivateAccount(@PathVariable Long id) {
        treasuryCommandPort.inactivate(id);
        return ResponseDto.<String>builder()
                .data("Account inactivated")
                .status(200)
                .message("Account inactivated successfully").build().of();
    }
}
