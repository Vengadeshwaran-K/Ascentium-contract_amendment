package com.company.contractsystem.contract.service;

import com.company.contractsystem.approval.repository.ApprovalMappingRepository;
import com.company.contractsystem.audit.service.AuditService;
import com.company.contractsystem.common.dto.DashboardStats;
import com.company.contractsystem.contract.dto.CreateContractRequest;
import com.company.contractsystem.contract.entity.Contract;
import com.company.contractsystem.contract.entity.ContractStatus;
import com.company.contractsystem.contract.entity.ContractVersion;
import com.company.contractsystem.contract.repository.ContractRepository;
import com.company.contractsystem.contract.repository.ContractVersionRepository;
import com.company.contractsystem.enums.RoleType;
import com.company.contractsystem.user.entity.User;
import com.company.contractsystem.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractVersionRepository versionRepository;
    private final UserRepository userRepository;
    private final VersionService versionService;
    private final ApprovalMappingRepository mappingRepository;
    private final AuditService auditService;

    public ContractService(ContractRepository contractRepository,
            ContractVersionRepository versionRepository,
            UserRepository userRepository,
            VersionService versionService,
            ApprovalMappingRepository mappingRepository,
            AuditService auditService) {
        this.contractRepository = contractRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
        this.versionService = versionService;
        this.mappingRepository = mappingRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Contract create(CreateContractRequest request, User creator) {
        // Fix Approval Mapping: Validate that the legal user has a mapping for the
        // selected client
        List<com.company.contractsystem.approval.entity.ApprovalMapping> mappings = mappingRepository
                .findByLegalUser(creator);
        if (mappings.isEmpty()) {
            throw new RuntimeException(
                    "No approval mapping found for you. Please contact Admin to setup your workflow mapping before creating contracts.");
        }

        User client = userRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        boolean hasMapping = mappings.stream().anyMatch(m -> m.getClientUser().getId().equals(client.getId()));
        if (!hasMapping) {
            throw new RuntimeException("You do not have an approval mapping for this client. Please contact Admin.");
        }

        if (request.getContractAmount() != null
                && request.getContractAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new RuntimeException("u should not enter negative numbers");
        }

        Contract contract = new Contract();
        contract.setContractName(request.getContractName());
        contract.setClient(client);
        contract.setEffectiveDate(request.getEffectiveDate());
        contract.setContractAmount(request.getContractAmount());
        contract.setCreatedAt(LocalDateTime.now());
        contract.setStatus(ContractStatus.DRAFT);

        Contract savedContract = contractRepository.save(contract);
        versionService.createInitialVersion(savedContract, creator);

        auditService.log("CONTRACT_CREATED",
                "Contract created: " + savedContract.getContractName());

        return savedContract;
    }

    @Transactional
    public void submit(Long id, User currentUser) {
        ContractVersion latest = versionService.findLatestVersion(id);
        if (latest == null || (latest.getStatus() != ContractStatus.DRAFT &&
                latest.getStatus() != ContractStatus.REJECTED_BY_FINANCE &&
                latest.getStatus() != ContractStatus.REJECTED_BY_CLIENT)) {
            throw new RuntimeException("Contract cannot be submitted in its current state");
        }

        if (currentUser.getRole().getName() == RoleType.LEGAL_USER) {
            // Fix Approval Mapping: Ensure mapping exists before submitting
            List<com.company.contractsystem.approval.entity.ApprovalMapping> mappings = mappingRepository
                    .findByLegalUser(currentUser);
            if (mappings.isEmpty()) {
                throw new RuntimeException(
                        "No approval mapping found for you. Please contact Admin to map your workflow.");
            }
            latest.setStatus(ContractStatus.PENDING_FINANCE);
        } else if (currentUser.getRole().getName() == RoleType.FINANCE_REVIEWER) {
            latest.setStatus(ContractStatus.PENDING_CLIENT);
        } else {
            throw new RuntimeException("Only Legal Users or Finance Reviewers can submit contracts");
        }

        latest.setUpdatedAt(LocalDateTime.now());
        versionRepository.save(latest);

        auditService.log("CONTRACT_SUBMITTED",
                "Contract submitted (ID: " + id + ") to next stage: " + latest.getStatus());
    }

    @Transactional
    public void approve(Long id, String remarks, User reviewer) {
        ContractVersion latest = versionService.findLatestVersion(id);
        if (latest == null)
            throw new RuntimeException("Version not found");

        if (latest.getStatus() == ContractStatus.PENDING_FINANCE) {
            latest.setStatus(ContractStatus.PENDING_CLIENT);
        } else if (latest.getStatus() == ContractStatus.PENDING_CLIENT) {
            latest.setStatus(ContractStatus.ACTIVE);
        } else {
            throw new RuntimeException("Contract not in a state to be approved");
        }

        latest.setRemarks(remarks);
        latest.setUpdatedAt(LocalDateTime.now());
        versionRepository.save(latest);

        auditService.log("CONTRACT_APPROVED",
                "Contract approved (ID: " + id + "). Status: " + latest.getStatus() + ". Remarks: " + remarks);
    }

    @Transactional
    public void reject(Long id, String remarks, User reviewer) {
        ContractVersion latest = versionService.findLatestVersion(id);
        ContractStatus currentStatus = latest.getStatus();

        if (currentStatus == ContractStatus.PENDING_FINANCE) {
            latest.setStatus(ContractStatus.REJECTED_BY_FINANCE);
            latest.setRemarks(remarks);
            latest.setUpdatedAt(LocalDateTime.now());
            versionRepository.save(latest);

            // Find the original legal user (who created V1)
            User legalUser = versionRepository.findByContractIdAndVersionNumber(latest.getContract().getId(), 1)
                    .map(ContractVersion::getCreator)
                    .orElse(latest.getCreator());

            // Create a NEW version for Legal to fix
            versionService.createNewVersion(latest.getContract(),
                    latest.getVersionNumber() + 1,
                    ContractStatus.REJECTED_BY_FINANCE,
                    "Finance Rejected: " + remarks,
                    legalUser);

            auditService.log("CONTRACT_REJECTED",
                    "Contract rejected by Finance (ID: " + id + "). Remarks: " + remarks);
        } else if (currentStatus == ContractStatus.PENDING_CLIENT) {
            latest.setStatus(ContractStatus.REJECTED_BY_CLIENT);
            latest.setRemarks(remarks);
            latest.setUpdatedAt(LocalDateTime.now());
            versionRepository.save(latest);

            // Find the mapped Finance Reviewer for this contract
            // We look at the version 1 creator to find the mapping
            User legalUser = versionRepository.findByContractIdAndVersionNumber(latest.getContract().getId(), 1)
                    .map(ContractVersion::getCreator)
                    .orElse(latest.getCreator());

            List<com.company.contractsystem.approval.entity.ApprovalMapping> mappings = mappingRepository
                    .findByLegalUser(legalUser);
            User financeUser = mappings.stream()
                    .filter(m -> m.getClientUser().getId().equals(latest.getContract().getClient().getId()))
                    .map(m -> m.getFinanceUser())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "No Finance Reviewer mapped to this contract's creator and client"));

            // Create a NEW version for Finance to fix
            versionService.createNewVersion(latest.getContract(),
                    latest.getVersionNumber() + 1,
                    ContractStatus.REJECTED_BY_CLIENT,
                    "Client Rejected: " + remarks,
                    financeUser);

            auditService.log("CONTRACT_REJECTED",
                    "Contract rejected by Client (ID: " + id + "). Remarks: " + remarks);
        } else {
            throw new RuntimeException("Contract not in a state to be rejected");
        }
    }

    @Transactional(readOnly = true)
    public List<ContractVersion> getMyContracts(User user) {
        // First group by contract to find the true latest version of each contract
        return versionRepository.findAll().stream()
                .collect(Collectors.groupingBy(v -> v.getContract().getId()))
                .values().stream()
                .map(list -> list.stream()
                        .max((v1, v2) -> Integer.compare(v1.getVersionNumber(), v2.getVersionNumber()))
                        .orElse(null))
                .filter(v -> v != null && v.getCreator().getId().equals(user.getId()))
                .filter(v -> {
                    if (user.getRole().getName() == RoleType.LEGAL_USER) {
                        return v.getStatus() == ContractStatus.DRAFT ||
                                v.getStatus() == ContractStatus.REJECTED_BY_FINANCE;
                    } else if (user.getRole().getName() == RoleType.FINANCE_REVIEWER) {
                        return v.getStatus() == ContractStatus.REJECTED_BY_CLIENT;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Contract update(Long id, CreateContractRequest request) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        ContractVersion latest = versionService.findLatestVersion(id);
        if (latest == null || (latest.getStatus() != ContractStatus.DRAFT &&
                latest.getStatus() != ContractStatus.REJECTED_BY_FINANCE &&
                latest.getStatus() != ContractStatus.REJECTED_BY_CLIENT)) {
            throw new RuntimeException("Contract can only be updated in DRAFT or REJECTED state");
        }

        if (request.getContractAmount() != null
                && request.getContractAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new RuntimeException("u should not enter negative numbers");
        }

        contract.setContractName(request.getContractName());
        contract.setEffectiveDate(request.getEffectiveDate());
        contract.setContractAmount(request.getContractAmount());

        Contract saved = contractRepository.save(contract);
        auditService.log("CONTRACT_UPDATED",
                "Contract updated (ID: " + id + ") in " + latest.getStatus() + " state.");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ContractVersion> getApprovalQueue(User user) {
        List<ContractStatus> pendingStatuses = List.of(ContractStatus.PENDING_FINANCE, ContractStatus.PENDING_CLIENT);
        return versionRepository.findByStatusIn(pendingStatuses).stream()
                .filter(v -> {
                    if (v.getStatus() == ContractStatus.PENDING_FINANCE) {
                        // Check if current user is one of the mapped finance reviewers for the creator
                        return mappingRepository.findByLegalUser(v.getCreator()).stream()
                                .anyMatch(m -> m.getFinanceUser().getId().equals(user.getId()));
                    } else if (v.getStatus() == ContractStatus.PENDING_CLIENT) {
                        // Check if current user is the client of the contract
                        return v.getContract().getClient().getId().equals(user.getId());
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractVersion> getAllActiveContracts(User user) {
        RoleType role = user.getRole().getName();

        // Only Admin and Client should see the Approved Contracts tab content
        if (role != RoleType.SUPER_ADMIN && role != RoleType.CLIENT) {
            return List.of();
        }

        return versionRepository.findByStatusIn(List.of(ContractStatus.ACTIVE)).stream()
                .filter(v -> {
                    if (role == RoleType.SUPER_ADMIN) {
                        return true;
                    }
                    // For Client: only show their own contracts
                    return v.getContract().getClient().getId().equals(user.getId());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats(User user) {
        RoleType role = user.getRole().getName();
        Map<String, Long> counters = new HashMap<>();
        List<ContractVersion> allLatest = versionRepository.findAll().stream()
                .collect(Collectors.groupingBy(v -> v.getContract().getId()))
                .values().stream()
                .map(list -> list.stream()
                        .max((v1, v2) -> Integer.compare(v1.getVersionNumber(), v2.getVersionNumber())).get())
                .collect(Collectors.toList());

        if (role == RoleType.SUPER_ADMIN) {
            counters.put("Total Contracts", contractRepository.count());
            counters.put("Approved Contracts", allLatest.stream()
                    .filter(v -> v.getStatus() == ContractStatus.ACTIVE).count());
            counters.put("Waiting List", allLatest.stream()
                    .filter(v -> v.getStatus() == ContractStatus.PENDING_FINANCE
                            || v.getStatus() == ContractStatus.PENDING_CLIENT)
                    .count());

            Map<String, Long> userCounts = userRepository.findAll().stream()
                    .collect(Collectors.groupingBy(u -> u.getRole().getName().toString(), Collectors.counting()));
            for (Map.Entry<String, Long> entry : userCounts.entrySet()) {
                counters.put("Users: " + entry.getKey(), entry.getValue());
            }
        } else if (role == RoleType.LEGAL_USER) {
            List<ContractVersion> myContracts = allLatest.stream()
                    .filter(v -> {
                        // Check if user created V1
                        return versionRepository.findAll().stream()
                                .anyMatch(av -> av.getContract().getId().equals(v.getContract().getId())
                                        && av.getVersionNumber() == 1
                                        && av.getCreator().getId().equals(user.getId()));
                    }).collect(Collectors.toList());

            counters.put("Contracts Created", (long) myContracts.size());
            counters.put("Sent to Finance", myContracts.stream()
                    .filter(v -> v.getStatus() == ContractStatus.PENDING_FINANCE).count());
            counters.put("Approved", myContracts.stream()
                    .filter(v -> v.getStatus() == ContractStatus.ACTIVE).count());
            counters.put("Rejected by Finance", myContracts.stream()
                    .filter(v -> v.getStatus() == ContractStatus.REJECTED_BY_FINANCE)
                    .count());
        } else if (role == RoleType.FINANCE_REVIEWER) {
            List<ContractVersion> mappedContracts = allLatest.stream()
                    .filter(v -> {
                        // Find V1 creator
                        User creator = versionRepository.findAll().stream()
                                .filter(av -> av.getContract().getId().equals(v.getContract().getId())
                                        && av.getVersionNumber() == 1)
                                .findFirst().map(av -> av.getCreator()).orElse(null);
                        if (creator == null)
                            return false;
                        return mappingRepository.findByLegalUser(creator).stream()
                                .anyMatch(m -> m.getFinanceUser().getId().equals(user.getId()));
                    }).collect(Collectors.toList());

            counters.put("Pending My Review", mappedContracts.stream()
                    .filter(v -> v.getStatus() == ContractStatus.PENDING_FINANCE).count());
            counters.put("Approved by Me", mappedContracts.stream()
                    .filter(v -> v.getStatus() == ContractStatus.PENDING_CLIENT
                            || v.getStatus() == ContractStatus.ACTIVE)
                    .count());
            counters.put("Rejected by Me (to Legal)", mappedContracts.stream()
                    .filter(v -> v.getStatus() == ContractStatus.REJECTED_BY_FINANCE).count());
            counters.put("Rejected by Client (Fix required)", mappedContracts.stream()
                    .filter(v -> v.getCreator().getId().equals(user.getId())
                            && v.getStatus() == ContractStatus.REJECTED_BY_CLIENT)
                    .count());
        } else if (role == RoleType.CLIENT) {
            List<ContractVersion> myClientContracts = allLatest.stream()
                    .filter(v -> v.getContract().getClient().getId().equals(user.getId()))
                    .collect(Collectors.toList());

            counters.put("Pending My Review", myClientContracts.stream()
                    .filter(v -> v.getStatus() == ContractStatus.PENDING_CLIENT).count());
            counters.put("Approved by Me", myClientContracts.stream()
                    .filter(v -> v.getStatus() == ContractStatus.ACTIVE).count());
            counters.put("Rejected by Me", myClientContracts.stream()
                    .filter(v -> v.getStatus() == ContractStatus.REJECTED_BY_CLIENT).count());
        }

        return DashboardStats.builder()
                .counters(counters)
                .role(role.toString())
                .build();
    }

    @Transactional(readOnly = true)
    public List<User> getMappedClients(User legalUser) {
        return mappingRepository.findByLegalUser(legalUser).stream()
                .map(com.company.contractsystem.approval.entity.ApprovalMapping::getClientUser)
                .distinct()
                .collect(Collectors.toList());
    }
}
