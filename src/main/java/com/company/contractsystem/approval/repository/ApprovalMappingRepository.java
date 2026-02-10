package com.company.contractsystem.approval.repository;

import com.company.contractsystem.approval.entity.ApprovalMapping;
import com.company.contractsystem.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalMappingRepository extends JpaRepository<ApprovalMapping, Long> {

    Optional<ApprovalMapping> findByLegalUser(User legalUser);

    Optional<ApprovalMapping> findByFinanceUser(User financeUser);
}
