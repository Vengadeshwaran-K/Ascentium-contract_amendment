package com.company.contractsystem.audit.service;

import com.company.contractsystem.audit.entity.AuditLog;
import com.company.contractsystem.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String action, String actor, String remarks) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setActor(actor);
        log.setRemarks(remarks);
        log.setTimestamp(LocalDateTime.now());
        repository.save(log);
    }
}
