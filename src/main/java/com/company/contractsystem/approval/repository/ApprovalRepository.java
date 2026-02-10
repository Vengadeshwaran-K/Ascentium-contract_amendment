package com.company.contractsystem.approval.repository;

import com.company.contractsystem.approval.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
}
