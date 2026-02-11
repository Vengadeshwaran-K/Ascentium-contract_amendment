package com.company.contractsystem.audit.service;

import com.company.contractsystem.audit.entity.AuditLog;
import com.company.contractsystem.audit.repository.AuditLogRepository;
import com.company.contractsystem.user.entity.User;
import jakarta.persistence.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String action, String remarks) {
        String actor = "SYSTEM";
        String actorRole = "SYSTEM";

        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) auth
                    .getPrincipal();
            actor = userDetails.getUsername();
            actorRole = userDetails.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("USER");
        }

        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setActor(actor);
        log.setActorRole(actorRole);
        log.setRemarks(remarks);
        log.setTimestamp(LocalDateTime.now());
        repository.save(log);
    }

    public java.util.List<AuditLog> getAllLogs() {
        return repository.findAll();
    }
}
