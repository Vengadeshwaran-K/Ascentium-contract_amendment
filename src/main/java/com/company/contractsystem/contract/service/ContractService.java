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
    public void submit(Long id) {
        ContractVersion latest = versionService.findLatestVersion(id);
        if (latest == null || (latest.getStatus() != ContractStatus.DRAFT &&
                latest.getStatus() != ContractStatus.REJECTED_BY_FINANCE &&
                latest.getStatus() != ContractStatus.REJECTED_BY_CLIENT)) {
            throw new RuntimeException("Contract cannot be submitted in its current state");
        }
        latest.setStatus(ContractStatus.PENDING_FINANCE);
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
        if (latest == null)
            throw new RuntimeException("Version not found");

        ContractStatus currentStatus = latest.getStatus();
        if (currentStatus == ContractStatus.PENDING_FINANCE) {
            latest.setStatus(ContractStatus.REJECTED_BY_FINANCE);
        } else if (currentStatus == ContractStatus.PENDING_CLIENT) {
            latest.setStatus(ContractStatus.REJECTED_BY_CLIENT);
        } else {
            throw new RuntimeException("Contract not in a state to be rejected");
        }
        latest.setRemarks(remarks);
        latest.setUpdatedAt(LocalDateTime.now());
        versionRepository.save(latest);

        // Create a NEW version for Legal to fix
        versionService.createNewVersion(latest.getContract(),
                latest.getVersionNumber() + 1,
                ContractStatus.DRAFT,
                "Resubmission required: " + remarks,
                latest.getCreator());
    }

    public List<ContractVersion> getMyContracts(User user) {
        // Find contracts where user is creator or client or the mapped finance reviewer
        return versionRepository.findAll().stream()
                .filter(v -> v.getCreator().getId().equals(user.getId())
                        || v.getContract().getClient().getId().equals(user.getId())
                        || mappingRepository.findByLegalUser(v.getCreator())
                                .map(m -> m.getFinanceUser().getId().equals(user.getId()))
                                .orElse(false))
                .collect(Collectors.groupingBy(v -> v.getContract().getId()))
                .values().stream()
                .map(list -> list.stream()
                        .max((v1, v2) -> Integer.compare(v1.getVersionNumber(), v2.getVersionNumber())).get())
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
}
