package com.treasury.infrastructure.adapters.input.rest.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class TransferDtoRequest {
    private Long sourceAccountId;
    private Long destinationAccountId;
    private Double amount;
}
