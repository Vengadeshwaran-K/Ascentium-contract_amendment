package com.company.contractsystem.contract.repository;

import com.company.contractsystem.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {
}
