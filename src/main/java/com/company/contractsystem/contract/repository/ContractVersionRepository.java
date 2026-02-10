package com.company.contractsystem.contract.repository;

import com.company.contractsystem.contract.entity.ContractVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractVersionRepository extends JpaRepository<ContractVersion, Long> {
}
