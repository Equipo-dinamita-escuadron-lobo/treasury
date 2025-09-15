package com.treasury.infrastructure.adapters.output.exception.dto;

import org.springframework.http.ResponseEntity;

import lombok.Builder;

@Builder
public class ErrorResponseDto<T> {
    private int errorCode;
    private String message;
    private T data;

    public ResponseEntity<ErrorResponseDto<T>> of() {
        return ResponseEntity.status(errorCode).body(this);
    }

    // Getters
    public int getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
