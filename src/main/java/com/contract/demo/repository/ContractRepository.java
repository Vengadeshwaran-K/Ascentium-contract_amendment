package com.contract.demo.repository;

import com.contract.demo.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByAssignedRole(String assignedRole);
}
