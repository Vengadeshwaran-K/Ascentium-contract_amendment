package com.company.contractsystem.approval.service;

import com.company.contractsystem.approval.entity.Approval;
import com.company.contractsystem.approval.repository.ApprovalRepository;
import com.company.contractsystem.enums.ApprovalDecision;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ApprovalService {

    private final ApprovalRepository repository;

    public ApprovalService(ApprovalRepository repository) {
        this.repository = repository;
    }

    public Approval approve(Long contractId, String role, String remarks) {
        Approval approval = new Approval();
        approval.setContractId(contractId);
        approval.setDecision(ApprovalDecision.APPROVED);
        approval.setReviewerRole(role);
        approval.setRemarks(remarks);
        approval.setReviewedAt(LocalDateTime.now());
        return repository.save(approval);
    }

    public Approval reject(Long contractId, String role, String remarks) {
        Approval approval = new Approval();
        approval.setContractId(contractId);
        approval.setDecision(ApprovalDecision.REJECTED);
        approval.setReviewerRole(role);
        approval.setRemarks(remarks);
        approval.setReviewedAt(LocalDateTime.now());
        return repository.save(approval);
    }
}
