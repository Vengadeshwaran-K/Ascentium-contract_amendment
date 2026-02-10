package com.company.contractsystem.contract.service;

import com.company.contractsystem.contract.entity.ContractVersion;
import com.company.contractsystem.contract.repository.ContractVersionRepository;
import org.springframework.stereotype.Service;

@Service
public class VersionService {

    private final ContractVersionRepository repository;

    public VersionService(ContractVersionRepository repository) {
        this.repository = repository;
    }

    public ContractVersion createVersion(Long contractId, int version) {
        ContractVersion cv = new ContractVersion();
        cv.setContractId(contractId);
        cv.setVersionNumber(version);
        cv.setActive(false);
        return repository.save(cv);
    }
}
