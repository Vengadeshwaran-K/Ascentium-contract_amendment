package com.company.contractsystem.contract.controller;

import com.company.contractsystem.contract.dto.CreateContractRequest;
import com.company.contractsystem.contract.entity.Contract;
import com.company.contractsystem.contract.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @PostMapping
    public Contract create(@Valid @RequestBody CreateContractRequest name) {
        return service.create(name);
    }
}
