package com.company.contractsystem.contract.service;

import com.company.contractsystem.approval.repository.ApprovalMappingRepository;
import com.company.contractsystem.contract.dto.CreateContractRequest;
import com.company.contractsystem.contract.entity.Contract;
import com.company.contractsystem.contract.entity.ContractStatus;
import com.company.contractsystem.contract.entity.ContractVersion;
import com.company.contractsystem.contract.repository.ContractRepository;
import com.company.contractsystem.contract.repository.ContractVersionRepository;
import com.company.contractsystem.user.entity.User;
import com.company.contractsystem.user.repository.UserRepository;
import com.company.contractsystem.enums.RoleType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractVersionRepository versionRepository;
    private final UserRepository userRepository;
    private final VersionService versionService;
    private final ApprovalMappingRepository mappingRepository;

    public ContractService(ContractRepository contractRepository,
            ContractVersionRepository versionRepository,
            UserRepository userRepository,
            VersionService versionService,
            ApprovalMappingRepository mappingRepository) {
        this.contractRepository = contractRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
        this.versionService = versionService;
        this.mappingRepository = mappingRepository;
    }

    @Transactional
    public Contract create(CreateContractRequest request, User creator) {
        User client = userRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Contract contract = new Contract();
        contract.setContractName(request.getContractName());
        contract.setClient(client);
        contract.setEffectiveDate(request.getEffectiveDate());
        contract.setContractAmount(request.getContractAmount());
        contract.setCreatedAt(LocalDateTime.now());
        contract.setStatus(ContractStatus.DRAFT);

        Contract savedContract = contractRepository.save(contract);
        versionService.createInitialVersion(savedContract, creator);

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

        if (currentUser.getRole().getName() == com.company.contractsystem.enums.RoleType.LEGAL_USER) {
            latest.setStatus(ContractStatus.PENDING_FINANCE);
        } else if (currentUser.getRole().getName() == com.company.contractsystem.enums.RoleType.FINANCE_REVIEWER) {
            latest.setStatus(ContractStatus.PENDING_CLIENT);
        } else {
            throw new RuntimeException("Only Legal Users or Finance Reviewers can submit contracts");
        }

        latest.setUpdatedAt(LocalDateTime.now());
        versionRepository.save(latest);
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
            User legalUser = versionRepository.findAll().stream()
                    .filter(v -> v.getContract().getId().equals(latest.getContract().getId()))
                    .filter(v -> v.getVersionNumber() == 1)
                    .findFirst()
                    .map(v -> v.getCreator())
                    .orElse(latest.getCreator());

            // Create a NEW version for Legal to fix
            versionService.createNewVersion(latest.getContract(),
                    latest.getVersionNumber() + 1,
                    ContractStatus.REJECTED_BY_FINANCE,
                    "Finance Rejected: " + remarks,
                    legalUser);

        } else if (currentStatus == ContractStatus.PENDING_CLIENT) {
            latest.setStatus(ContractStatus.REJECTED_BY_CLIENT);
            latest.setRemarks(remarks);
            latest.setUpdatedAt(LocalDateTime.now());
            versionRepository.save(latest);

            // Find the mapped Finance Reviewer for this contract
            // We look at the version 1 creator to find the mapping
            User legalUser = versionRepository.findAll().stream()
                    .filter(v -> v.getContract().getId().equals(latest.getContract().getId()))
                    .filter(v -> v.getVersionNumber() == 1)
                    .findFirst()
                    .map(v -> v.getCreator())
                    .orElse(latest.getCreator());

            User financeUser = mappingRepository.findByLegalUser(legalUser)
                    .map(m -> m.getFinanceUser())
                    .orElseThrow(() -> new RuntimeException("No Finance Reviewer mapped to this contract's creator"));

            // Create a NEW version for Finance to fix
            versionService.createNewVersion(latest.getContract(),
                    latest.getVersionNumber() + 1,
                    ContractStatus.REJECTED_BY_CLIENT,
                    "Client Rejected: " + remarks,
                    financeUser);
        } else {
            throw new RuntimeException("Contract not in a state to be rejected");
        }
    }

    @Transactional(readOnly = true)
    public List<ContractVersion> getMyContracts(User user) {
        // Only show latest versions where the user is the current responsible party
        // (creator)
        // and the contract is not yet active or submitted.
        return versionRepository.findAll().stream()
                .collect(Collectors.groupingBy(v -> v.getContract().getId()))
                .values().stream()
                .map(list -> list.stream()
                        .max((v1, v2) -> Integer.compare(v1.getVersionNumber(), v2.getVersionNumber())).get())
                .filter(v -> v.getCreator().getId().equals(user.getId()))
                .filter(v -> v.getStatus() == ContractStatus.DRAFT ||
                        v.getStatus() == ContractStatus.REJECTED_BY_FINANCE ||
                        v.getStatus() == ContractStatus.REJECTED_BY_CLIENT)
                .collect(Collectors.toList());
    }

    @Transactional
    public Contract update(Long id, CreateContractRequest request) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        ContractVersion latest = versionService.findLatestVersion(id);
        if (latest == null || (latest.getStatus() != ContractStatus.DRAFT &&
                !latest.getStatus().toString().startsWith("REJECTED"))) {
            throw new RuntimeException("Contract can only be updated in DRAFT or REJECTED state");
        }

        contract.setContractName(request.getContractName());
        contract.setEffectiveDate(request.getEffectiveDate());
        contract.setContractAmount(request.getContractAmount());

        return contractRepository.save(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractVersion> getApprovalQueue(User user) {
        return versionRepository.findAll().stream()
                .filter(v -> {
                    if (v.getStatus() == ContractStatus.PENDING_FINANCE) {
                        // Check if current user is the mapped finance reviewer for the creator
                        return mappingRepository.findByLegalUser(v.getCreator())
                                .map(m -> m.getFinanceUser().getId().equals(user.getId()))
                                .orElse(false);
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

        return versionRepository.findAll().stream()
                .filter(v -> v.getStatus() == ContractStatus.ACTIVE)
                .filter(v -> {
                    if (role == RoleType.SUPER_ADMIN) {
                        return true;
                    }
                    // For Client: only show their own contracts
                    return v.getContract().getClient().getId().equals(user.getId());
                })
                .collect(Collectors.toList());
    }
}
