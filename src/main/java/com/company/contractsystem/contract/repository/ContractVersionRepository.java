package com.company.contractsystem.contract.repository;

import com.company.contractsystem.contract.entity.ContractStatus;
import com.company.contractsystem.contract.entity.ContractVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractVersionRepository extends JpaRepository<ContractVersion, Long> {
    Optional<ContractVersion> findByContractIdAndVersionNumber(Long contractId, int versionNumber);

    List<ContractVersion> findByContractId(Long contractId);

    @Query("SELECT cv FROM ContractVersion cv WHERE cv.status IN :statuses")
    List<ContractVersion> findByStatusIn(@Param("statuses") List<ContractStatus> statuses);
}
