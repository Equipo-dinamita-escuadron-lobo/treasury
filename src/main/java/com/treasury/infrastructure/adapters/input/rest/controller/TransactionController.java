package com.treasury.infrastructure.adapters.input.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.treasury.infrastructure.adapters.input.rest.dto.ResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/treasury/transactions")
public class TransactionController {

    @GetMapping("/health")
    public ResponseEntity<ResponseDto<String>> health() {
        return ResponseDto.<String>builder()
                .data("Treasury Transaction Service is running")
                .status(200)
                .message("Service is healthy").build().of();
    }

    // TODO: Implementar endpoints para transactions y splits
}
