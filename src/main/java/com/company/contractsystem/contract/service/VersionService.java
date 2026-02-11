package com.company.contractsystem.contract.service;

import com.company.contractsystem.contract.entity.Contract;
import com.company.contractsystem.contract.entity.ContractStatus;
import com.company.contractsystem.contract.entity.ContractVersion;
import com.company.contractsystem.contract.repository.ContractVersionRepository;
import com.company.contractsystem.user.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VersionService {

    private final ContractVersionRepository repository;

    public VersionService(ContractVersionRepository repository) {
        this.repository = repository;
    }

    public ContractVersion createInitialVersion(Contract contract, User creator) {
        ContractVersion cv = new ContractVersion();
        cv.setContract(contract);
        cv.setVersionNumber(1);
        cv.setStatus(ContractStatus.DRAFT);
        cv.setCreator(creator);
        cv.setUpdatedAt(LocalDateTime.now());
        cv.setRemarks("Initial version");
        cv.setActive(true);
        return repository.save(cv);
    }

    public ContractVersion createNewVersion(Contract contract, int versionNumber, ContractStatus status, String remarks,
            User creator) {
        ContractVersion cv = new ContractVersion();
        cv.setContract(contract);
        cv.setVersionNumber(versionNumber);
        cv.setStatus(status);
        cv.setRemarks(remarks);
        cv.setCreator(creator);
        cv.setUpdatedAt(LocalDateTime.now());
        cv.setActive(true);
        return repository.save(cv);
    }

    public ContractVersion findLatestVersion(Long contractId) {
        // Simple implementation for now, can be optimized with custom query
        return repository.findAll().stream()
                .filter(v -> v.getContract().getId().equals(contractId))
                .max((v1, v2) -> Integer.compare(v1.getVersionNumber(), v2.getVersionNumber()))
                .orElse(null);
    }
}
