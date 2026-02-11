package com.company.contractsystem.approval.repository;

import com.company.contractsystem.approval.entity.ApprovalMapping;
import com.company.contractsystem.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalMappingRepository extends JpaRepository<ApprovalMapping, Long> {

    java.util.List<ApprovalMapping> findByLegalUser(User legalUser);

    java.util.List<ApprovalMapping> findByFinanceUser(User financeUser);

    java.util.Optional<ApprovalMapping> findByLegalUserAndFinanceUserAndClientUser(User legal, User finance,
            User client);
}
