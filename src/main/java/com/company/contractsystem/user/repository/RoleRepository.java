package com.company.contractsystem.user.repository;

import com.company.contractsystem.enums.RoleType;
import com.company.contractsystem.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}
