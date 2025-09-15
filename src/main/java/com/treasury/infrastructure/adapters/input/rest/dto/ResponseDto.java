package com.treasury.infrastructure.adapters.input.rest.dto;

import org.springframework.http.ResponseEntity;

import lombok.Builder;

@Builder
public class ResponseDto<T> {
    private T data;
    private int status;
    private String message;

    public ResponseEntity<ResponseDto<T>> of() {
        return ResponseEntity.status(status).body(this);
    }

    // Getters
    public T getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
