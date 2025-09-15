package com.treasury.infrastructure.adapters.input.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class TreasuryDtoRequest {
    @JsonIgnore
    private Long id;

    private String accountNumber;

    private Double balance;

    private String currency;

    private String accountType;

    private boolean status = true;
}
