package com.company.contractsystem.approval.service;

import com.company.contractsystem.approval.dto.CreateApprovalMappingRequest;
import com.company.contractsystem.approval.entity.ApprovalMapping;
import com.company.contractsystem.approval.repository.ApprovalMappingRepository;
import com.company.contractsystem.user.entity.User;
import com.company.contractsystem.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ApprovalMappingService {

    private final ApprovalMappingRepository mappingRepository;
    private final UserRepository userRepository;

    public ApprovalMappingService(
            ApprovalMappingRepository mappingRepository,
            UserRepository userRepository
    ) {
        this.mappingRepository = mappingRepository;
        this.userRepository = userRepository;
    }

    public ApprovalMapping createMapping(CreateApprovalMappingRequest request) {

        User legalUser = userRepository.findById(request.getLegalUserId())
                .orElseThrow(() -> new RuntimeException("Legal user not found"));

        User financeUser = userRepository.findById(request.getFinanceUserId())
                .orElseThrow(() -> new RuntimeException("Finance user not found"));

        User clientUser = userRepository.findById(request.getClientUserId())
                .orElseThrow(() -> new RuntimeException("Client user not found"));

        mappingRepository.findByLegalUser(legalUser).ifPresent(mapping -> {
            throw new RuntimeException("Mapping already exists for this legal user");
        });

        ApprovalMapping mapping = new ApprovalMapping();
        mapping.setLegalUser(legalUser);
        mapping.setFinanceUser(financeUser);
        mapping.setClientUser(clientUser);

        return mappingRepository.save(mapping);
    }

    public ApprovalMapping getByLegalUser(User legalUser) {
        return mappingRepository.findByLegalUser(legalUser)
                .orElseThrow(() -> new RuntimeException("No approval mapping found for legal user"));
    }

    public ApprovalMapping getByFinanceUser(User financeUser) {
        return mappingRepository.findByFinanceUser(financeUser)
                .orElseThrow(() -> new RuntimeException("No approval mapping found for finance user"));
    }
}
