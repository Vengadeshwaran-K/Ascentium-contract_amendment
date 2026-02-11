package com.company.contractsystem.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateContractRequest {
    @NotBlank
    private String contractName;
    private Long clientId;
    @NotNull
    private LocalDate effectiveDate;
    @NotNull
    @jakarta.validation.constraints.Min(0)
    private BigDecimal contractAmount;
}
