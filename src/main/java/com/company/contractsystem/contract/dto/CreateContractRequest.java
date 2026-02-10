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
    @NotNull
    private Long clientId;
    @NotNull
    private LocalDate effectiveDate;
    @NotNull
    private BigDecimal contractAmount;
}
