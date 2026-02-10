package com.company.contractsystem.approval.controller;

import com.company.contractsystem.approval.entity.Approval;
import com.company.contractsystem.approval.service.ApprovalService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService service;

    public ApprovalController(ApprovalService service) {
        this.service = service;
    }

    @PostMapping("/approve")
    public Approval approve(@RequestParam Long contractId,
                            @RequestParam String role,
                            @RequestParam String remarks) {
        return service.approve(contractId, role, remarks);
    }

    @PostMapping("/reject")
    public Approval reject(@RequestParam Long contractId,
                           @RequestParam String role,
                           @RequestParam String remarks) {
        return service.reject(contractId, role, remarks);
    }
}
