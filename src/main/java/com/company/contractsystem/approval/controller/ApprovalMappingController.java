package com.company.contractsystem.approval.controller;

import com.company.contractsystem.approval.dto.CreateApprovalMappingRequest;
import com.company.contractsystem.approval.entity.ApprovalMapping;
import com.company.contractsystem.approval.service.ApprovalMappingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/approval-mappings")
public class ApprovalMappingController {

    private final ApprovalMappingService mappingService;

    public ApprovalMappingController(ApprovalMappingService mappingService) {
        this.mappingService = mappingService;
    }

    /**
     * SUPER ADMIN API
     * Maps Legal -> Finance -> Client
     */
    @PostMapping
    public ApprovalMapping createMapping(
            @Valid @RequestBody CreateApprovalMappingRequest request
    ) {
        return mappingService.createMapping(request);
    }
}
