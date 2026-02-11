package com.company.contractsystem.approval.service;

import com.company.contractsystem.approval.dto.CreateApprovalMappingRequest;
import com.company.contractsystem.approval.entity.ApprovalMapping;
import com.company.contractsystem.approval.repository.ApprovalMappingRepository;
import com.company.contractsystem.audit.service.AuditService;
import com.company.contractsystem.user.entity.User;
import com.company.contractsystem.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ApprovalMappingService {

        private final ApprovalMappingRepository mappingRepository;
        private final UserRepository userRepository;
        private final AuditService auditService;

        public ApprovalMappingService(
                        ApprovalMappingRepository mappingRepository,
                        UserRepository userRepository,
                        AuditService auditService) {
                this.mappingRepository = mappingRepository;
                this.userRepository = userRepository;
                this.auditService = auditService;
        }

        /**
         * SUPER ADMIN creates approval chain:
         * Legal -> Finance -> Client
         */
        public ApprovalMapping createMapping(CreateApprovalMappingRequest request) {

                User legalUser = userRepository.findById(request.getLegalUserId())
                                .orElseThrow(() -> new RuntimeException("Legal user not found"));

                User financeUser = userRepository.findById(request.getFinanceUserId())
                                .orElseThrow(() -> new RuntimeException("Finance user not found"));

                User clientUser = userRepository.findById(request.getClientUserId())
                                .orElseThrow(() -> new RuntimeException("Client user not found"));

                // ❌ Prevent EXACT duplicate mapping (Triplet: Legal + Finance + Client)
                mappingRepository.findByLegalUserAndFinanceUserAndClientUser(legalUser, financeUser, clientUser)
                                .ifPresent(mapping -> {
                                        throw new RuntimeException("This exact approval mapping already exists.");
                                });

                ApprovalMapping mapping = new ApprovalMapping();
                mapping.setLegalUser(legalUser);
                mapping.setFinanceUser(financeUser);
                mapping.setClientUser(clientUser);

                ApprovalMapping saved = mappingRepository.save(mapping);
                auditService.log("WORKFLOW_MAPPING_CREATED",
                                String.format("Created approval chain: Legal(%s) -> Finance(%s) -> Client(%s)",
                                                legalUser.getUsername(), financeUser.getUsername(),
                                                clientUser.getUsername()));

                return saved;
        }

        /**
         * Used later during Legal Submit
         */
        public java.util.List<ApprovalMapping> getByLegalUser(User legalUser) {
                return mappingRepository.findByLegalUser(legalUser);
        }

        /**
         * Used later during Finance Approval
         */
        public java.util.List<ApprovalMapping> getByFinanceUser(User financeUser) {
                return mappingRepository.findByFinanceUser(financeUser);
        }
}
